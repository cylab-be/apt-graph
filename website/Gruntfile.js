module.exports = function(grunt) {
 
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        
        jshint: {
            all: ['js/*.js']
        },
        eslint: {
            src: ["js/*.js"]
        },
        htmlhint: {
            html1: {
              options: {
                "tagname-lowercase": true,
                "attr-lowercase": true,
                "attr-value-double-quotes": true,
                "doctype-first": true,
                "tag-pair": true,
                "spec-char-escape": true,
                "id-unique": true,
                "src-not-empty": true,
                "attr-no-duplication": true,
                "title-require": true
              },
              src: ['*.html']
            }
          },
          jscs: {
              src: "js/*.js",
              options: {
                  "preset": "google"
              }
          }
    });

    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks("gruntify-eslint");
    grunt.loadNpmTasks('grunt-htmlhint');
    grunt.loadNpmTasks("grunt-jscs");

    grunt.registerTask('default', ['jshint', 'eslint', 'htmlhint', 'jscs']); 
};
