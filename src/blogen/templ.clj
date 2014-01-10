(ns blogen.templ
  (:use
   [blogen.config])
  (:require
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

(html/defsnippet footer-template (from-template "_footer.html") [html/root]
  [word-count]
  [:#word-count] (html/content word-count))

(html/defsnippet header-template (from-template "_header.html") [html/root]
  [site-title]
  [:#site-title] (html/content site-title))

(html/defsnippet head-template (from-template "_head.html") [:head]
  [title style-link]
  [:title] (html/content (make-title title))
  [:link] (html/substitute style-link)
  )

(html/deftemplate post-template (from-template "post.html")
  [head header footer content]
  [:head] (html/content head)
  [:#header] (html/content header)
  [:#footer] (html/content footer)
  [:#content] (html/substitute content))

;; Actual interface to single posts
(defn single-post
  ""
  [post]
  (post-template
   (head-template (:title post)
                  (link-to-css (:path-depth post)))
   (header-template (:site-title @config))
   (footer-template (str (:word-count post)))
   (:content post)))
