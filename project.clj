(defproject rest-cljer "0.1.23-SNAPSHOT"
  :description "A Clojure wrapper for the rest driver library"
  :url "https://github.com/whostolebenfrog/rest-cljer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[cheshire "5.5.0"]
                 [clj-http "0.5.2"]
                 [com.github.rest-driver/rest-client-driver "1.1.42" :exclusions [org.slf4j/slf4j-nop]]
                 [environ "0.3.0"]
                 [junit "4.11"]
                 [midje "1.5.1"]
                 [org.clojure/clojure "1.4.0"]
                 [speclj "3.1.0"]]

  :lein-release {:deploy-via :clojars}

  :profiles {:dev {:plugins [[lein-midje "3.1.0"]
                             [lein-release "1.0.5"]
                             [lein-rpm "0.0.4"]]}})
