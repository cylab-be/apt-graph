module.exports = function(grunt) {
 
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        jshint: {
            all: ['Gruntfile.js', 'js/graph.js']
        }
    });
    grunt.loadNpmTasks('grunt-contrib-jshint');
 
    grunt.registerTask('default', ['jshint']);
 
};
