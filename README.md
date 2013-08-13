# rest-cljer

A Clojure wrapper for [rest-driver](https://github.com/rest-driver/rest-driver).

## Usage

Import from [clojars](https://clojars.org/rest-cljer) with:

```clj
[rest-cljer "0.1.8"]
```

then

```clj
(:require [rest-cljer.core :refer [rest-driven]])
```

Example below shows midje usage.

```clj
(fact "Example of testing two"
      (rest-driven
          [{:method :GET :url "/gety"}
           {:type :JSON :status 200}

           {:method :GET :url "/something"}
           {:type :JSON :status 200}]

          (client/get "http://localhost:8081/gety") => (contains {:status 200})
          (client/get "http://localhost:8081/something") => (contains {:status 200})))
```

Wrap your test with the `(rest-driven)` macro.

This expects params in the form of a vector of pairs of maps followed by a body form that is your test.

The two maps correspond to a request and response (in that order). The request tells us what request to expect and the response map describes the response.

Another example:

```clj
(fact "User history is posted to scrobbling"
       (rest-driven
           [{:method :POST :url "/events"
             :body [(Pattern/compile "\\{.*\"userid\":\"userid\".*\\}")
                    "application/json"]}
            {:type :JSON :status 202}]

           (let [response (client/post ...snip...)]

             response => (contains {:status 202}))))
```

You can also specific a map as the body of the request, thereby asserting that the right content is sent:

```clj
(fact "I want to know my code is sending out the right information"
       (rest-driven
           [{:method :POST :url "/downstream"
             :body {:information "yes,please"}
            {:type :JSON :status 202}]

           (let [response (client/post {:body (json-str {:infomation "yes,please"}) :content-type :json]

             response => (contains {:status 202}))))
```

There is also some sweetening of response definitions, like so:

```clj
(rest-driven [{:method :GET :url resource-path}
                      {:body {:inigo "montoya"}}]
                     (let [resp (http/get url)]
                       resp => (contains {:status 200})
                       (:headers resp) => (contains {"content-type" "application/json"})
                       (json/read-str (:body resp) :key-fn keyword) => {:inigo "montoya"}))
```


Request map params:

    :method  -> :GET :POST :PUT :DELETE :TRACE :HEAD :OPTIONS
    :body    -> a string or regex or map that should match the body of the request
    :url     -> a string or regex that should match the url
    :headers -> a map of headers that are expected on the incoming request (where
                key is header name and value is header value).
    :and     -> a function that will recieve a ClientDriverRequest and can apply
                additional rest-driver setup steps that aren't explicitly supported
                by rest-cljer.

Response map params:

    :type    -> :JSON (application/json) :XML (text/xml) :PLAIN (text/plain)
    :status  -> the response status as a number
    :body    -> a response body (string)
    :headers -> a map of headers that will be added to the response (where key is
                header name and value is header value).
    :and     -> a function that will recieve a ClientDriverResponse and can apply
                additional rest-driver setup steps that aren't explicitly supported
                by rest-cljer.
    :times   -> for repeating responses, the number of times a matching request will
                be given this response (use :times :any to use this response for an
                unlimited number of matching requests).

## License

Distributed under the Eclipse Public License, the same as Clojure.
