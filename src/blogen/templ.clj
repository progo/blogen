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
  (str template-dir "/" filename))
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
     :href (with-depth depth main-css-location)}]))

(html/deftemplate post-template (from-template "post.html")
  [head-extra title content]
  [:head] (html/append head-extra)
  [:title] (html/content title)
  [:#content] (html/substitute content))

;; Actual interface to single posts
(defn single-post
  ""
  [post]
  (post-template
   (link-to-css (:path-depth post))
   (:title post)
   (:content post)))