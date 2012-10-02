(ns rest-cljer.core
  (:require [midje.sweet :refer :all]
            [environ.core :refer [env]])

  (:import [com.github.restdriver.clientdriver ClientDriverFactory ClientDriverRule]
           [com.github.restdriver.clientdriver RestClientDriver ClientDriverRequest$Method]))

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

(defn add-params [request params]
  (doseq [k (keys params)]
    (.withParam request k (params k))))

(defn add-body [request body]
  (when-not (nil? body)
    (.withBody request (first body) (second body))))

(defmacro rest-driven
  ([pairs & body]
     `(let [driver# (.. (ClientDriverFactory.) (createClientDriver (Integer. (env :restdriver-port))))]
        (try
          (doseq [pair# (partition 2 ~pairs)]
            (let [request# (first pair#)
                  response# (second pair#)
                  on-request#    (.. (RestClientDriver/onRequestTo (:url request#))
                                      (withMethod ((:method request#) verbs)))
                  give-response# (.. (RestClientDriver/giveResponse (get response# :body ""))
                                      (withStatus (:status response#))
                                      (withContentType (content-type (:type response#))))]

              (add-params on-request# (:params request#))
              (add-body   on-request# (:body   request#))

              (.addExpectation driver# on-request# give-response#)))

          ~@body

          (try (.verify driver#) (catch Exception e# (fact e# => nil)))

          (finally (.shutdownQuietly driver#))))))
