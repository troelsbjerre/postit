/*jshint esversion: 6 */

document.addEventListener("DOMContentLoaded", () => { 
    document.getElementById('board').onkeydown = (e) => {
        if (e.keyCode == 13) {
            window.location.assign('http://postit.itu.dk/' + document.getElementById('board').value);
        }
    };
});