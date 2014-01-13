(ns blogen.templ
  (:use
   [blogen.config])
  (:require
   [clj-time.format]
   [net.cgrand.enlive-html :as html]))

(defn- streamify 
  [path]
  (java.io.FileReader. path))
(defn- with-template-dir
  [filename]
  (str (:template-dir @config) "/" filename))
(defn- from-template
  "Take just a file name and produce a FileReader object for enlive to
  use."
  [template-name]
  (streamify
   (with-template-dir template-name)))

(defn with-depth
  "Provide a relative path (presumably) with parent dirs."
  [depth s]
  (str (apply str (repeat depth "../"))
       s))

(defn- format-date-with-fmt
  "Format a DateTime object to human readable string, using
  `format-string'."
  [format-string dt]
  (clj-time.format/unparse
   (clj-time.format/formatter format-string)
   dt))

(def format-date
  "DateTime (just date) -> human readable"
  (partial format-date-with-fmt
           (:date-format @config)))

(def format-datetime
  "DateTime (date and time) -> human readable"
  (partial format-date-with-fmt
           (:datetime-format @config)))

(defn link-to-css
  [depth]
  (html/html
   [:link
    {:rel "stylesheet"
     :type "text/css"
     :href (with-depth depth (:main-css-location @config))}]))

(defn make-title
  [s]
  (str s " - " (:site-title @config)))

(defn build-tags
  [tags]
  (html/html
   (for [t tags]
     [:li (str t)])))

(html/defsnippet footer-template (from-template "_footer.html") [html/root]
  [post])

(html/defsnippet header-template (from-template "_header.html") [html/root]
  [post]
  [:#site-title] (html/content (:site-title @config)))

(html/defsnippet head-template (from-template "_head.html") [:head]
  [post]
  [:title] (html/content (make-title (:title post)))
  [:link] (html/substitute (link-to-css (:path-depth post))))

(html/defsnippet sidebar-template (from-template "_sidebar.html") [html/root]
  [post]
  [:#post-created] (html/content (format-date (:created post)))
  [:#tags] (html/content (build-tags (:tags post)))
  [:#post-modified] (html/content (-> post
                                      :history
                                      first
                                      :date
                                      format-datetime)))

(html/deftemplate post-template (from-template "post.html")
  [post]
  [:head] (html/content (head-template post))
  [:#header] (html/content (header-template post))
  [:#sidebar] (html/content (sidebar-template post))
  [:#footer] (html/content (footer-template post))
  [:#content] (html/substitute (:content post)))

(defn single-post
  [post]
  (post-template post))
