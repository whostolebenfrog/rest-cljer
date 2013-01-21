(ns rest-cljer.test.core
  (:require [rest-cljer.core :refer [rest-driven]]
            [midje.sweet :refer :all]
            [clj-http.client :refer [post]]
            [environ.core :refer [env]])
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
