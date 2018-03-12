(ns rest-cljer.test.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [clojure.test :refer :all]
            [rest-cljer.core :refer [*rest-driver-port* json-capture rest-driven string-capture]])
  (:import [com.github.restdriver.clientdriver ClientDriver ClientDriverRequest$Method]
           [com.github.restdriver.clientdriver.exception ClientDriverFailedExpectationException]
           [java.net SocketTimeoutException]))

(defn local-path
  "Returns a URI to a resource on a free port, on localhost with the supplied postfix"
  [postfix]
  (str "http://localhost:" *rest-driver-port* postfix))

(deftest rest-driven-call-succeeds-without-exceptions
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path))))))))

(deftest rest-driven-call-returns-body-result
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path))))))))

(deftest rest-driven-call-with-binary-body-succeeds-without-exceptions
  (let [resource-path "/some/resource/path"
        bytes (byte-array [(byte 10) (byte 20) (byte 30)])]
    (rest-driven [{:method :POST
                   :url resource-path}
                  {:status 200 :body bytes}]
      (is (= (seq bytes) (-> (http/post (local-path resource-path)) :body (.getBytes) seq))))))

(deftest unexpected-rest-driven-call-should-fail-with-exception
  (is (thrown? RuntimeException (rest-driven [] (http/post (local-path "/"))))))

(deftest test-json-document-matching
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body {:ping "pong"}}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path)
                                     {:content-type :json
                                      :body (json/generate-string {:ping "pong"})
                                      :throw-exceptions false})))))))

(deftest check-body-via-predicate
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body #(apply = (map sort [[3 2 1] %]))}
                  {:status 204}]
      (is (= 204) (:status (http/post (local-path resource-path)
                                      {:content-type :json
                                       :body (json/generate-string [1 3 2])
                                       :throw-exceptions false}))))))

(deftest check-body-via-predicate-with-type-as-string
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body [#(= "Hi" %) "application/text"]}
                  {:status 204}]
      (is (= 204) (:status (http/post (local-path resource-path)
                                      {:content-type "application/text"
                                       :body "Hi"
                                       :throw-exceptions false}))))))

(deftest check-body-via-predicate-order-independent
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body [#(= "Hi" %) "application/text"]}
                  {:status 204}
                  {:method :POST
                   :url resource-path
                   :body [#(= "not-hi" %) "application/text"]}
                  {:status 204}]
      (http/post (local-path resource-path) {:content-type "application/text"
                                             :body "not-hi"
                                             :throw-exceptions false})
      (is (= 204 (:status (http/post (local-path resource-path)
                                     {:content-type "application/text"
                                      :body "Hi"
                                      :throw-exceptions false})))))))

(deftest json-document-capture-as-a-string
  (let [resource-path "/some/resource/path"
        capturer (string-capture)]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body {:ping "pong"}
                   :capture capturer}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path)
                                     {:content-type :json
                                      :body (json/generate-string {:ping "pong"})
                                      :throw-exceptions false}))))
      (is (= "{\"ping\":\"pong\"}" (capturer))))))

(deftest json-document-captured-and-parsed-using-json-capture
  (let [resource-path "/some/resource/path"
        capturer (json-capture)]
    (rest-driven [{:method :POST
                   :url resource-path
                   :body {:ping "pong"}
                   :capture capturer}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path)
                                     {:content-type :json
                                      :body (json/generate-string {:ping "pong"})
                                      :throw-exceptions false}))))
      (is (= {:ping "pong"} (capturer))))))

(deftest sweetening-of-response-definitions
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :GET
                   :url resource-path}
                  {:body {:inigo "montoya"}}]
      (let [{:keys [status body headers]} (http/get (local-path resource-path))]
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= {:inigo "montoya"} (json/parse-string body true)))))))

(deftest sweetening-of-response-does-not-override-explicit-http-status
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :GET
                   :url resource-path}
                  {:status 400
                   :body {:inigo "montoya"}}]
      (let [{:keys [status body headers]} (http/get (local-path resource-path)
                                                    {:throw-exceptions false})]
        (is (= 400 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= {:inigo "montoya"} (json/parse-string body true)))))))

(deftest post-processing-of-request-and-response-replacing-initial-values-with-new-ones-using-the-:and-function
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :GET
                   :url resource-path
                   :and #(.withMethod % ClientDriverRequest$Method/POST)}
                  {:status 204 :and #(.withStatus % 205)}]
      (is (= 205 (:status (http/post (local-path resource-path))))))))

(deftest response-can-be-repeated-any-times
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :PUT
                   :url resource-path}
                  {:status 204 :times :any}]
      (dotimes [n 3]
        (is (= 204 (:status (http/put (local-path resource-path)))))))))

(deftest response-can-be-repeated-a-specific-number-of-times
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path}
                  {:status 200 :times 2}]
      (dotimes [n 2]
        (is (= 200 (:status (http/post (local-path resource-path)))))))
    (is (thrown? Exception (rest-driven [{:method :POST :url resource-path}
                                         {:status 200 :times 2}]
                             (dotimes [n 2]
                               (is (= 200 (:status (http/post (local-path resource-path))))))
                             (http/post (local-path resource-path)))))))

(deftest rest-driven-call-with-expected-header-succeeds
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :headers {"from" "mytest"
                             "with" "value"}}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path)
                                     {:headers {"from" "mytest"
                                                "with" "value"}})))))))

(deftest rest-driven-call-with-expected-header-succeeds-with-keyord-header-names
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :headers {:from "myfrom" :with "value"}}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path) {:headers {"from" "myfrom"
                                                                           "with" "value"}})))))))

(deftest rest-driven-call-with-missing-header-throws-exception
  (let [resource-path "/some/resource/path"]
    (is (thrown? Exception (rest-driven [{:method :POST
                                          :url resource-path
                                          :headers {"From" "origin"}}
                                         {:status 204}]
                             (http/post (local-path resource-path)))))))

(deftest rest-driven-call-without-header-that-is-expected-to-be-absent-succeeds
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :not {:headers {"myheader" "myvalue"}}}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path))))))))

(deftest rest-driven-call-with-header-that-is-expected-to-be-absent-throw-exception
  (let [resource-path "/some/resource/path"]
    (is (thrown? RuntimeException (rest-driven [{:method :POST
                                                 :url resource-path
                                                 :not {:headers {"myheader" "myvalue"}}}
                                                {:status 204}]
                                    (http/post (local-path resource-path)
                                               {:headers {"myheader" "myvalue"}}))))))

(deftest rest-driven-call-with-vector-of-headers-that-are-expected-to-be-absent-succeeds
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path
                   :not {:headers ["myheader"]}}
                  {:status 204}]
      (is (= 204 (:status (http/post (local-path resource-path))))))))

(deftest rest-driven-call-with-vector-of-headers-that-are-expected-to-be-absent-throw-exception
  (let [resource-path "/some/resource/path"]
    (is (thrown? RuntimeException (rest-driven [{:method :POST
                                                 :url resource-path
                                                 :not {:headers ["myheader"]}}
                                                {:status 204}]
                                    (http/post (local-path resource-path)
                                               {:headers {"myheader" "myvalue"}}))))))

(deftest rest-driven-call-with-response-headers-succeeds
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :POST
                   :url resource-path}
                  {:status 204
                   :headers {"from" "rest-cljer", "with" "value"}}]
      (let [{:keys [status headers]} (http/post (local-path resource-path))]
        (is (= 204 status))
        (is (= "rest-cljer" (get headers "From")))
        (is (= "value" (get headers "with")))))))

(deftest can-supply-params-using-keywords-as-well-as-strings
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :GET
                   :url resource-path
                   :params {:a "a" "b" "b"}}
                  {:status 200}]
      (is (= 200 (:status (http/get (local-path (str resource-path "?a=a&b=b")))))))))

(deftest can-specify-:any-params
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method :GET
                   :url resource-path
                   :params :any}
                  {:status 200}]
      (is (= 200 (:status (http/get (local-path resource-path))))))))

(deftest request-method-is-:GET-by-default
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:url resource-path}
                  {:status 200}]
      (is (= 200 (:status (http/get (local-path resource-path))))))))

(deftest request-response-can-be-paired-as-a-vector
  (let [resource-path "/some/resource/path"]
    (rest-driven [[{:url resource-path} {:status 201}]
                  [{:url resource-path} {:status 202}]]
      (is (= 201 (:status (http/get (local-path resource-path)))))
      (is (= 202 (:status (http/get (local-path resource-path))))))))

(deftest expected-rest-driven-call-with-an-unusual-http-method-succeeds
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:method "PATCH"
                   :url resource-path}
                  {:status 204}]
      (is (= 204 (:status (http/patch (local-path resource-path))))))))

(deftest expected-rest-driven-call-times-out-if-after-is-to-larger-than-socket-timeout
  (let [resource-path "/some/resource/path"]
    (rest-driven [{:url resource-path}
                  {:status 200
                   :after 200}]
      (is (thrown? SocketTimeoutException (http/get (local-path resource-path)
                                                    {:socket-timeout 100}))))))

(deftest can-use-environ-to-provide-port
  (let [port (ClientDriver/getFreePort)
        original-env env]
    (try
      (alter-var-root (var env) assoc :restdriver-port port)
      (let [resource-path "/some/resource/path"]
        (rest-driven [{:method :POST :url resource-path}
                      {:status 204}]
          (is (= 204 (:status (http/post (str "http://localhost:" port resource-path)))))))
      (finally
        (alter-var-root (var env) (constantly original-env))))))
