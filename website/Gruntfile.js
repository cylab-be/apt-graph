module.exports = function(grunt) {
 
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        jshint: {
            all: ['Gruntfile.js', 'js/graph.js']
        },
        jscs: {
            src: "js/graph.js"        
        },
        clean: {
            js: ['*.min.js']
        },
        githooks: {
            all: {
              'pre-commit': 'default' 
            }       
        }
    });
    
    grunt.loadNpmTasks('grunt-jscs');
    grunt.loadNpmTasks('grunt-githooks');
    grunt.loadNpmTasks('grunt-contrib-jshint');
    grunt.loadNpmTasks('grunt-contrib-clean');
 
    grunt.registerTask('default', ['clean', 'jshint', 'jscs']);
 
};
