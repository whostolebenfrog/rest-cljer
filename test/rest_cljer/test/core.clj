(ns rest-cljer.test.core
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]
            [rest-cljer.core :refer [*rest-driver-port* json-capture rest-driven string-capture]])
  (:import [com.github.restdriver.clientdriver ClientDriver ClientDriverRequest$Method]
           [com.github.restdriver.clientdriver.exception ClientDriverFailedExpectationException]
           [java.net SocketTimeoutException]))

(defn local-path
  "Returns a URI to a resource on a free port, on localhost with the supplied postfix"
  [postfix]
  (str "http://localhost:" *rest-driver-port* postfix))

(fact-group
 :acceptance

 (fact "expected rest-driven call succeeds without exceptions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST :url resource-path}
                       {:status 204}]
           (http/post (local-path resource-path)) => (contains {:status 204}))))

 (fact "expected rest-driven call returns body result"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST :url resource-path}
                       {:status 204}]
           (http/post (local-path resource-path))) => (contains {:status 204})))

 (fact "rest-driven call with binary body succeeds without exceptions"
       (let [resource-path "/some/resource/path"
             bytes (byte-array [(byte 10) (byte 20) (byte 30)])]
         (rest-driven [{:method :POST :url resource-path}
                       {:status 200 :body bytes}]
           (-> (http/post (local-path resource-path)) :body (.getBytes) seq) => (seq bytes))))

 (fact "unexpected rest-driven call should fail with exception"
       (rest-driven [] (http/post (local-path "/"))) => (throws RuntimeException))

 (fact "test json document matching"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST :url resource-path
                        :body {:ping "pong"}}
                       {:status 204}]
           (http/post (local-path resource-path) {:content-type :json
                                                  :body (json/generate-string {:ping "pong"})
                                                  :throw-exceptions false}) => (contains {:status 204}))))

 (fact "check body via predicate"
       (let [resource-path "/some/resource/path"]
         (rest-driven
             [{:method :POST
               :url resource-path
               :body #(apply = (map sort [[3 2 1] %]))}
              {:status 204}]
             (http/post (local-path resource-path) {:content-type :json
                                                    :body (json/generate-string [1 3 2])
                                                    :throw-exceptions false}) => (contains {:status 204}))))

 (fact "check body via predicate - type as string"
       (let [resource-path "/some/resource/path"]
         (rest-driven
             [{:method :POST
               :url resource-path
               :body [#(= "Hi" %) "application/text"]}
              {:status 204}]
             (http/post (local-path resource-path) {:content-type "application/text"
                                                    :body "Hi"
                                                    :throw-exceptions false}) => (contains {:status 204}))))

 (fact "check body via predicate order independant"
       (let [resource-path "/some/resource/path"]
         (rest-driven
             [{:method :POST
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
             (http/post (local-path resource-path) {:content-type "application/text"
                                                    :body "Hi"
                                                    :throw-exceptions false}) => (contains {:status 204}))))

 (fact "test json document capture as a string"
       (let [resource-path "/some/resource/path"
             capturer (string-capture)]
         (rest-driven [{:method :POST :url resource-path
                        :body {:ping "pong"}
                        :capture capturer}
                       {:status 204}]
           (http/post (local-path resource-path) {:content-type :json
                                                  :body (json/generate-string {:ping "pong"})
                                                  :throw-exceptions false}) => (contains {:status 204})
           (capturer) => "{\"ping\":\"pong\"}")))

 (fact "test json document captured and parsed using json-capture"
       (let [resource-path "/some/resource/path"
             capturer (json-capture)]
         (rest-driven [{:method :POST :url resource-path
                        :body {:ping "pong"}
                        :capture capturer}
                       {:status 204}]
           (http/post (local-path resource-path) {:content-type :json
                                                  :body (json/generate-string {:ping "pong"})
                                                  :throw-exceptions false}) => (contains {:status 204})
           (capturer) => {:ping "pong"})))

 (fact "test sweetening of response definitions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :GET :url resource-path}
                       {:body {:inigo "montoya"}}]
           (let [resp (http/get (local-path resource-path))]
             resp => (contains {:status 200})
             (:headers resp) => (contains {"Content-Type" "application/json"})
             (json/parse-string (:body resp) true) => {:inigo "montoya"}))))

 (fact "sweetening of response doesn't override explicit http status"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :GET :url resource-path}
                       {:status 400
                        :body {:inigo "montoya"}}]
           (let [resp (http/get (local-path resource-path) {:throw-exceptions false})]
             resp => (contains {:status 400})
             (:headers resp) => (contains {"Content-Type" "application/json"})
             (json/parse-string (:body resp) true) => {:inigo "montoya"}))))

 (fact "test post-processing of request and response, replace initial values with new ones using :and function"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :GET :url resource-path :and #(.withMethod % ClientDriverRequest$Method/POST)}
                       {:status 204 :and #(.withStatus % 205)}]
           (http/post (local-path resource-path)) => (contains {:status 205}))))

 (fact "give repeated response any times"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :PUT :url resource-path}
                       {:status 204 :times :any}]
           (http/put (local-path resource-path)) => (contains {:status 204})
           (http/put (local-path resource-path)) => (contains {:status 204})
           (http/put (local-path resource-path)) => (contains {:status 204}))))

 (fact "give repeated response a specfic number of times"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST :url resource-path}
                       {:status 200 :times 2}]
           (http/post (local-path resource-path)) => (contains {:status 200})
           (http/post (local-path resource-path)) => (contains {:status 200}))
         (rest-driven [{:method :POST :url resource-path}
                       {:status 200 :times 2}]
           (http/post (local-path resource-path)) => (contains {:status 200})
           (http/post (local-path resource-path)) => (contains {:status 200})
           (http/post (local-path resource-path))) => (throws Exception)))

 (fact "rest-driven call with expected header succeeds without exceptions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :headers {"from" "midjefact", "with" "value"}}
                       {:status 204}]
                      (http/post (local-path resource-path) {:headers {"from" "midjefact", "with" "value"}}) => (contains {:status 204}))))

  (fact "rest-driven call with expected header succeeds without exceptions with keyword header names"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :headers {:from "midjefact" :with "value"}}
                       {:status 204}]
           (http/post (local-path resource-path) {:headers {"from" "midjefact", "with" "value"}}) => (contains {:status 204}))))

 (fact "rest-driven call with missing header throws exception"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :headers {"From" "origin"}}
                       {:status 204}]
           (http/post (local-path resource-path)) => (contains {:status 204}))) => (throws RuntimeException))

 (fact "rest-driven call without header that is expected to be absent succeeds without exceptions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :not {:headers {"myheader" "myvalue"}}}
                       {:status 204}]
           (http/post (local-path resource-path)) => (contains {:status 204}))))

 (fact "rest-driven call with header that is expected to be absent throws exception"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :not {:headers {"myheader" "myvalue"}}}
                       {:status 204}]
           (http/post (local-path resource-path) {:headers {"myheader" "myvalue"}}) => (contains {:status 204}))) => (throws RuntimeException))

 (fact "rest-driven call without headers that are expected to be absent (specified in a vector) succeeds without exceptions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :not {:headers ["myheader"]}}
                       {:status 204}]
           (http/post (local-path resource-path)) => (contains {:status 204}))))

 (fact "rest-driven call with headers that are expected to be absent (specified in a vector) throws exception"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path, :not {:headers ["myheader"]}}
                       {:status 204}]
           (http/post (local-path resource-path) {:headers {"myheader" "myvalue"}}) => (contains {:status 204}))) => (throws RuntimeException))

 (fact "rest-driven call with response headers succeeds without exceptions"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :POST, :url resource-path}
                       {:status 204, :headers {"from" "rest-cljer", "with" "value"}}]
           (let [response (http/post (local-path resource-path))]
             response => (contains {:status 204})
             (:headers response) => (contains {"From" "rest-cljer"})
             (:headers response) => (contains {"with" "value"})))))

 (fact "can supply params using keywords as well as strings"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :GET :url resource-path :params {:a "a" "b" "b"}}
                       {:status 200}]
           (let [response (http/get (local-path (str resource-path "?a=a&b=b")))]
             response => (contains {:status 200})))))

 (fact "can specify :any params"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method :GET :url resource-path :params :any}
                       {:status 200}]
           (let [response (http/get (local-path resource-path))]
             response => (contains {:status 200})))))

 (fact "request method is :GET by default"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:url resource-path}
                       {:status 200}]
           (let [response (http/get (local-path resource-path))]
             response => (contains {:status 200})))))

 (fact "request/response can be paired as a vector"
       (let [resource-path "/some/resource/path"]
         (rest-driven [[{:url resource-path} {:status 201}]
                       [{:url resource-path} {:status 202}]]
           (http/get (local-path resource-path)) => (contains {:status 201})
           (http/get (local-path resource-path)) => (contains {:status 202}))))

 (fact "expected rest-driven call with an unusual HTTP method succeeds"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:method "PATCH", :url resource-path}
                       {:status 204}]
           (http/patch (local-path resource-path)) => (contains {:status 204}))))

 (fact "expected rest-driven call times out if after is set to larger than socket-timeout"
       (let [resource-path "/some/resource/path"]
         (rest-driven [{:url resource-path}
                       {:status 200
                        :after 200}]
           (http/get (local-path resource-path) {:socket-timeout 100}) => (throws SocketTimeoutException))))

 (fact "can use environ to provide port"
       (let [port (ClientDriver/getFreePort)
             original-env env]
         (try
           (alter-var-root (var env) assoc :restdriver-port port)
           (let [resource-path "/some/resource/path"]
             (rest-driven [{:method :POST :url resource-path}
                           {:status 204}]
               (http/post (str "http://localhost:" port resource-path)) => (contains {:status 204})))
           (finally
             (alter-var-root (var env) (constantly original-env)))))))
