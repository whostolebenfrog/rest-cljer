(defproject clj-driver "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.slf4j/slf4j-api "1.6.4"]
                 [org.slf4j/jul-to-slf4j "1.6.0"]
                 [clj-http "0.5.2"]
                 [cheshire "4.0.1"]
                 [midje "1.4.0"]]
           
  :profiles {:dev {:dependencies [[clj-oauth "1.3.1-SNAPSHOT"]
                                  [com.github.rest-driver/rest-client-driver "1.1.22" :exclusions [org.slf4j/slf4j-nop]]
                                  [junit "4.10"] 
                                  [clj-http-fake "0.4.1"]]
                   :plugins [[lein-rpm "0.0.4"]
                             [lein-midje "2.0.0-SNAPSHOT"]]}}

)
