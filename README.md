# clj-driver

A Clojure wrapper for rest-driver

## Usage

(fact "Example of testing two"
   (rest-driven
       [{:method :GET :url "/gety"}
        {:type :JSON :status 200}

        {:method :GET :url "/something"}
        {:type :JSON :status 200}]

     (client/get "http://localhost:8081/gety") => (contains {:status 200})
     (client/get "http://localhost:8081/something") => (contains {:status 200})))

## License

Distributed under the Eclipse Public License, the same as Clojure.
