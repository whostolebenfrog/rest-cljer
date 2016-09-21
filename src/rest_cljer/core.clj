(ns rest-cljer.core
  (:require [cheshire.core :as json]
            [clojure.data :refer [diff]]
            [clojure.java.io :refer [input-stream]]
            [environ.core :refer [env]])
  (:import [com.github.restdriver.clientdriver ClientDriverFactory ClientDriverRule
            RestClientDriver ClientDriverRequest$Method]
           [com.github.restdriver.clientdriver.capture StringBodyCapture]
           [java.util.concurrent TimeUnit]
           [java.io InputStream]
           [org.hamcrest Matcher]))

(def verbs
  {:GET     (ClientDriverRequest$Method/GET)
   :DELETE  (ClientDriverRequest$Method/DELETE)
   :HEAD    (ClientDriverRequest$Method/HEAD)
   :OPTIONS (ClientDriverRequest$Method/OPTIONS)
   :POST    (ClientDriverRequest$Method/POST)
   :PUT     (ClientDriverRequest$Method/PUT)
   :TRACE   (ClientDriverRequest$Method/TRACE)})

(def types
  {:JSON  "application/json"
   :PLAIN "text/plain"
   :XML   "text/xml"})

(defn content-type
  [sym]
  (if (keyword? sym)
    (sym types)
    sym))

(defn- get-diffs
  [expected actual]
  (if (map? expected)
    (diff expected (try (json/parse-string actual true) (catch Exception e actual)))
    [expected actual]))

(defn- pred-matcher
  [request pred body]
  (proxy [org.hamcrest.Matcher] []
    (matches [item]
      (if (.matches (.getBodyContentType request) "application/json")
        (pred (json/parse-string item true))
        (pred item)))
    (describeTo [description]
      (doto description
        (.appendText (format "expected %s not received" (.getBodyContentType request)))))
    (describeMismatch [actual description]
      (let [difs (get-diffs body actual)]
        (doto description
          (.appendText (str "expected BODY has <" (first difs) ">, actual BODY has <" (second difs) "> ")))))))

(defn- map-matcher [request body]
  (pred-matcher request (partial = body) body))

(defn string-capture []
  (proxy [StringBodyCapture clojure.lang.IFn] []
    (invoke [] (proxy-super getContent))))

(defn json-capture []
  (proxy [StringBodyCapture clojure.lang.IFn] []
    (invoke [] (json/parse-string (proxy-super getContent) true))))

(defn add-param! [request param-name param-vals]
  (doseq [v param-vals]
    (.withParam request (name param-name) v)))

(defn add-params [request params]
  (if (= :any params) (.withAnyParams request)
    (doseq [k (keys params)]
      (if (vector? (params k))
        (add-param! request k (params k))
        (add-param! request k [(params k)])))))

(defn add-body [request body]
  (let [[body content-type] (if (vector? body)
                              body
                              [body "application/json"])]
    (when-not (nil? body)
      (cond
        (map? body) (.withBody request (map-matcher request body) content-type)
        (fn? body) (.withBody request (pred-matcher request body body) content-type)
        :else (.withBody request body content-type)))))

(defn add-times [expectation times]
  (cond (= :any times) (.anyTimes expectation)
        times (.times expectation times)))

(defn add-header! [r [k v]]
  (.withHeader r (name k) v))

(defn add-headers [r headers]
  (doseq [h headers]
    (add-header! r h)))

(defn add-absent-header! [r k]
  (.withoutHeader r (name k)))

(defn add-after  [r m]
  (when m
    (.after r m TimeUnit/MILLISECONDS)))

(defn add-absent-headers [r headers]
  "Add headers that shouldn't be present. The original approach was to add them as a map. However,
   the value of each header is irrelevant and, so, a second approach of specifying just the names
   of the headers as a vector has been added. The first apprach is retained for back compatibility."
  (if (map? headers)
    (doseq [h headers]
      (add-absent-header! r (first h)))
    (doseq [h headers]
      (add-absent-header! r h))))

(defn add-capture [r c]
  (.capturingBodyIn r c))

(defn sweeten-response [{:keys [status] :or {status 200} :as r} ]
  (if (map? (:body r))
    (assoc r :body (json/generate-string (:body r)) :type :JSON :status status)
    r))

(defn binary? [b]
  (or (instance? InputStream b) (instance? (type (byte-array [])) b)))

(defn create-response [b t]
  "Create a rest-driver response with body b (optional) and content-type
  t (optional) by dispatching to the correct method based on the
  presence and type of b and t."
  (cond (and b (binary? b)) (RestClientDriver/giveResponseAsBytes (input-stream b) t)
        (and b t) (RestClientDriver/giveResponse b t)
        b (RestClientDriver/giveResponse b)
        :else (RestClientDriver/giveEmptyResponse)))

(defn choose-method [{method :method}]
  (if (string? method)
    (ClientDriverRequest$Method/custom method)
    ((or method :GET) verbs)))

(defn create-client-driver
  []
  (if (env :restdriver-port)
    (.. (ClientDriverFactory.) (createClientDriver (Integer. (env :restdriver-port))))
    (.. (ClientDriverFactory.) (createClientDriver))))

(def ^:dynamic *rest-driver-port*
  nil)

(defmacro rest-driven
  {:style/indent 1}
  ([pairs & body]
   `(let [driver# (create-client-driver)]
      (binding [*rest-driver-port* (.getPort driver#)]
        (try
          (doseq [pair# (partition 2 (flatten ~pairs))]
            (let [request# (first pair#)
                  response# (sweeten-response (second pair#))
                  on-request#    (.. (RestClientDriver/onRequestTo (:url request#))
                                     (withMethod (choose-method request#)))
                  give-response# (.. (create-response (:body response#) (content-type (:type response#)))
                                     (withStatus (:status response#)))]

              (add-params  on-request# (:params  request#))
              (add-body    on-request# (:body    request#))
              (add-headers on-request# (:headers request#))
              (add-absent-headers on-request# (:headers (:not request#)))
              (add-capture on-request# (:capture request#))

              (add-after give-response# (:after response#))

              (when (fn? (:and request#)) ((:and request#) on-request#))
              (when (fn? (:and response#)) ((:and response#) give-response#))
              (add-headers give-response# (:headers response#))

              (let [expectation# (.addExpectation driver# on-request# give-response#)]
                (add-times expectation# (:times response#)))))

          (let [body-value# (do ~@body)]
            (.verify driver#)

            body-value#)

          (finally (.shutdownQuietly driver#)))))))
