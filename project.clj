(defproject rest-cljer "0.2.1"
  :description "A Clojure wrapper for the rest driver library"
  :url "https://github.com/whostolebenfrog/rest-cljer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[cheshire "5.7.0"]
                 [clj-http "2.3.0"]
                 [com.github.rest-driver/rest-client-driver "1.1.42" :exclusions [org.slf4j/slf4j-nop]]
                 [environ "1.1.0"]
                 [junit "4.11"]
                 [org.clojure/clojure "1.8.0"]]

  :lein-release {:deploy-via :clojars}

  :profiles {:dev {:plugins [[lein-midje "3.1.0"]
                             [pjstadig/humane-test-output "0.8.1"]
                             [lein-release "1.0.5"]
                             [lein-rpm "0.0.4"]]
                   :dependencies [[midje "1.5.1"]
                                  [speclj "3.3.2"]]}})
