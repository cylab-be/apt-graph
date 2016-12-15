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
                'tag-pair': true
              },
              src: ['*.html']
            }
          }
    });

    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks("gruntify-eslint");
    grunt.loadNpmTasks('grunt-htmlhint');

    grunt.registerTask('default', ['eslint', 'jshint', 'htmlhint']); 
};
