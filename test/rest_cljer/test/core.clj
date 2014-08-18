(ns rest-cljer.test.core
  (:require [rest-cljer.core :refer [rest-driven]]
            [midje.sweet :refer :all]
            [clj-http.client :as http :refer [post put get]]
            [environ.core :refer [env]]
            [clojure.data.json :refer [json-str read-str]])
  (:import [com.github.restdriver.clientdriver ClientDriver ClientDriverRequest$Method]))

(fact "expected rest-driven call succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path}
                      {:status 204}]
                     (post url) => (contains {:status 204}))))

(fact "rest-driven call with binary body succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)
            bytes (byte-array [(byte 10) (byte 20) (byte 30)])]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path}
                      {:status 200 :body bytes}]
                     (-> (post url) :body (.getBytes) seq) => (seq bytes))))

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

(fact "sweetening of response doesn't override explicit http status"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path}
                      {:status 400
                       :body {:inigo "montoya"}}]
                     (let [resp (http/get url {:throw-exceptions false})]
                       resp => (contains {:status 400})
                       (:headers resp) => (contains {"content-type" "application/json"})
                       (read-str (:body resp) :key-fn keyword) => {:inigo "montoya"}))))

(fact "test post-processing of request and response, replace initial values with new ones using :and function"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path :and #(.withMethod % ClientDriverRequest$Method/POST)}
                      {:status 204 :and #(.withStatus % 205)}]
                     (post url) => (contains {:status 205}))))

(fact "give repeated response any times"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :PUT :url resource-path}
                      {:status 204 :times :any}]
                     (put url) => (contains {:status 204})
                     (put url) => (contains {:status 204})
                     (put url) => (contains {:status 204}))))

(fact "give repeated response a specfic number of times"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path}
                      {:status 200 :times 2}]
                     (post url) => (contains {:status 200})
                     (post url) => (contains {:status 200})))
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST :url resource-path}
                      {:status 200 :times 2}]
                     (post url)
                     (post url)
                     (post url))) => (throws RuntimeException))

(fact "rest-driven call with expected header succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path, :headers {"from" "midjefact", "with" "value"}}
                      {:status 204}]
                     (post url {:headers {"from" "midjefact", "with" "value"}}) => (contains {:status 204}))))

(fact "rest-driven call with missing header throws exception"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path, :headers {"From" "origin"}}
                      {:status 204}]
                     (post url) => (contains {:status 204}))) => (throws RuntimeException))

(fact "rest-driven call without header that is expected to be absent succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path, :not {:headers {"myheader" "myvalue"}}}
                      {:status 204}]
                     (post url) => (contains {:status 204}))))

(fact "rest-driven call with header that is expected to be absent throws exception"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path, :not {:headers {"myheader" "myvalue"}}}
                      {:status 204}]
                     (post url {:headers {"myheader" "myvalue"}}) => (contains {:status 204}))) => (throws RuntimeException))

(fact "rest-driven call with response headers succeeds without exceptions"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :POST, :url resource-path}
                      {:status 204, :headers {"from" "rest-cljer", "with" "value"}}]
                     (let [response (post url)]
                       response => (contains {:status 204})
                       (:headers response) => (contains {"from" "rest-cljer"})
                       (:headers response) => (contains {"with" "value"})))))

(fact "can supply params using keywords as well as strings"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path "?a=a&b=b")]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path :params {:a "a" "b" "b"}}
                      {:status 200}]
                     (let [response (get url)]
                       response => (contains {:status 200})))))

(fact "can specify :any params"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path "?a=a&b=b")]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:method :GET :url resource-path :params :any}
                      {:status 200}]
                     (let [response (get url)]
                       response => (contains {:status 200})))))

(fact "request method is :GET by default"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [{:url resource-path}
                      {:status 200}]
                     (let [response (get url)]
                       response => (contains {:status 200})))))

(fact "request/response can be paired as a vector"
      (let [restdriver-port (ClientDriver/getFreePort)
            resource-path "/some/resource/path"
            url (str "http://localhost:" restdriver-port resource-path)]
        (alter-var-root (var env) assoc :restdriver-port restdriver-port)
        (rest-driven [[{:url resource-path} {:status 201}]
                      [{:url resource-path} {:status 202}]]
                     (get url) => (contains {:status 201})
                     (get url) => (contains {:status 202}))))
