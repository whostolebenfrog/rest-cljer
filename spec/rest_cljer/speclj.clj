(ns rest-cljer.speclj
  (:require [clj-http.client :as http]
            [environ.core :refer [env]]
            [rest-cljer.core :refer [rest-driven]]
            [rest-cljer.test.speclj :refer :all]
            [speclj.core :refer :all])
  (:import [com.github.restdriver.clientdriver ClientDriver ClientDriverRequest$Method]))

(describe "rest-driven calls are mocked correctly"
          (it "must return a 200 response"
              (let [restdriver-port (ClientDriver/getFreePort)
                    url (str "http://localhost:" restdriver-port "/test")]
                (alter-var-root (var env) assoc :restdriver-port restdriver-port)
                (rest-driven [{:method :GET :url "/test"}
                              {:status 200 :type :JSON}]
                             (should= 200 (:status (http/get url)))))))

(describe "unexpected calls fail"
          (it "should fail with an exception"
              (rest-driven []
                           (should-throw
                            (http/get "/nope")))))

(describe "unmet expectations fail"
          (it "should fail with an exception"
              (let [restdriver-port (ClientDriver/getFreePort)
                    url (str "http://localhost:" restdriver-port "/test")]
                (alter-var-root (var env) assoc :restdriver-port restdriver-port)
                (rest-driven [{:method :GET :url "/test"}
                              {:status 200 :times 1}]
                             (should= 200 (:status (http/get url)))
                             (should-throw (http/get url))))))
