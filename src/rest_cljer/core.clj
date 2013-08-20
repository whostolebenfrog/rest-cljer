(ns rest-cljer.core
  (:require [midje.sweet :refer :all]
            [environ.core :refer [env]]
            [clojure.data :refer [diff]]
            [clojure.data.json :refer [read-str json-str]])

  (:import [com.github.restdriver.clientdriver ClientDriverFactory ClientDriverRule]
           [com.github.restdriver.clientdriver RestClientDriver ClientDriverRequest$Method]
           [org.hamcrest.Matcher]))

(def verbs
  {:GET     (ClientDriverRequest$Method/GET)
   :POST    (ClientDriverRequest$Method/POST)
   :PUT     (ClientDriverRequest$Method/PUT)
   :DELETE  (ClientDriverRequest$Method/DELETE)
   :TRACE   (ClientDriverRequest$Method/TRACE)
   :HEAD    (ClientDriverRequest$Method/HEAD)
   :OPTIONS (ClientDriverRequest$Method/OPTIONS)})

(def types
  {:JSON  "application/json"
   :XML   "text/xml"
   :PLAIN "text/plain"})

(defn content-type
  [sym]
  (if (keyword? sym)
    (sym types)
    sym))

(defn- map-matcher [map]
  (proxy [org.hamcrest.Matcher] []
    (matches [item]
      (= (read-str item :key-fn keyword) map))
    (describeTo [description]
      (doto description
        (.appendText (str "expected json not received"))))
    (describeMismatch [actual description]
      (let [difs (diff map (read-str actual :key-fn keyword))]
        (doto description
          (.appendText (str "expected has <" (first difs) ">, actual has <" (second difs) ">")))))))

(defn add-param! [request param-name param-vals]
  (doseq [v param-vals]
    (.withParam request param-name v)))

(defn add-params [request params]
  (doseq [k (keys params)]
    (if (vector? (params k))
      (add-param! request k (params k))
      (add-param! request k [(params k)]))))

(defn add-body [request body]
  (when-not (nil? body)
    (if (map? body)
      (.withBody request (map-matcher body) "application/json")
      (.withBody request (first body) (second body)))))

(defn add-times [expectation times]
  (cond (= :any times) (.anyTimes expectation)
        times (.times expectation times)))

(defn add-header! [r [k v]]
  (.withHeader r k v))

(defn add-headers [r headers]
  (doseq [h headers]
    (add-header! r h)))

(defn sweeten-response [{:keys [status] :or {status 200} :as r} ]
  (println "Status" status "\n" r)
  (if (map? (:body r))
    (assoc r :body (json-str (:body r)) :type :JSON :status status)
    r))

(defmacro rest-driven
  ([pairs & body]
     `(let [driver# (.. (ClientDriverFactory.) (createClientDriver (Integer. (env :restdriver-port))))]
        (try
          (doseq [pair# (partition 2 ~pairs)]
            (let [request# (first pair#)
                  response# (sweeten-response (second pair#))
                  on-request#    (.. (RestClientDriver/onRequestTo (:url request#))
                                      (withMethod ((:method request#) verbs)))
                  give-response# (.. (RestClientDriver/giveResponse (get response# :body ""))
                                      (withStatus (:status response#))
                                      (withContentType (content-type (:type response#))))]

              (add-params  on-request# (:params  request#))
              (add-body    on-request# (:body    request#))
              (add-headers on-request# (:headers request#))

              (when (fn? (:and request#)) ((:and request#) on-request#))
              (when (fn? (:and response#)) ((:and response#) give-response#))
              (add-headers give-response# (:headers response#))

              (let [expectation# (.addExpectation driver# on-request# give-response#)]
                (add-times expectation# (:times response#)))))

          ~@body

          (try (.verify driver#) (catch Exception e# (fact e# => nil)))

          (finally (.shutdownQuietly driver#))))))
