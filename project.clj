(defproject rest-cljer "0.2.2-SNAPSHOT"
  :description "A Clojure wrapper for the rest driver library"
  :url "https://github.com/whostolebenfrog/rest-cljer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[cheshire "5.8.0"]
                 [clj-http "3.8.0"]
                 [com.github.rest-driver/rest-client-driver "1.1.42" :exclusions [org.slf4j/slf4j-nop]]
                 [environ "1.1.0"]
                 [junit "4.12"]
                 [org.clojure/clojure "1.8.0"]]

  :deploy-repositories [["releases" :clojars]]

  :profiles {:dev {:plugins [[pjstadig/humane-test-output "0.8.2"]
                             [lein-rpm "0.0.4"]]
                   :dependencies [[speclj "3.3.2"]]}})
