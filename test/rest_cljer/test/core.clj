(ns rest-cljer.test.core
  (:require [rest-cljer.core :refer [rest-driven]]
            [midje.sweet :refer :all]
            [clj-http.client :as http :refer [post] :only [get]]
            [environ.core :refer [env]]
            [clojure.data.json :refer [json-str read-str]])
  (:import [com.github.restdriver.clientdriver ClientDriver]))

(fact "expected rest-driven call succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path}
                      {:status 204}]
                     (post url) => (contains {:status 204}))))

(fact "unexpected rest-driven call should fail with exception"
      (let [restdriver-port (ClientDriver/getFreePort)
            url (str "http://localhost:" restdriver-port)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [] (post url))) => (throws RuntimeException))

(fact "test json document matching"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path
                       :body {:ping "pong"}}
                      {:status 204}]
                     (post url {:content-type :json
                                :body (json-str {:ping "pong"})
                                :throw-exceptions false}) => (contains {:status 204}))))

(fact "test sweetening of response definitions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path}
                      {:body {:inigo "montoya"}}]
                     (let [resp (http/get url)]
                       resp => (contains {:status 200})
                       (:headers resp) => (contains {"content-type" "application/json"})
                       (read-str (:body resp) :key-fn keyword) => {:inigo "montoya"}))))
