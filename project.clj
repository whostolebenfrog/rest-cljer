(defproject rest-cljer "0.1.12-SNAPSHOT"
  :description "A Clojure wrapper for the rest driver library"
  :url "https://github.com/whostolebenfrog/rest-cljer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [environ "0.3.0"]
                 [junit "4.11"]
                 [clj-http "0.5.2"]
                 [midje "1.5.1"]
                 [org.clojure/data.json "0.2.1"]
                 [com.github.rest-driver/rest-client-driver "1.1.30" :exclusions [org.slf4j/slf4j-nop]]]

  :lein-release {:deploy-via :clojars}

  :profiles {:dev {:plugins [[lein-rpm "0.0.4"]
                             [lein-midje "3.1.0"]
		             [lein-release "1.0.5"]]}})
