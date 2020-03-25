/*jshint esversion: 6 */

function submit() {
    console.log('Sending question to server.');
    fetch(document.location.href, {
        method: 'POST',
        body: document.getElementById('question').value
    });
    document.getElementById('question').value = '';
}

document.addEventListener("DOMContentLoaded", () => { 
    document.getElementById('submit').onclick = submit;
    document.getElementById('question').onkeydown = (e) => {
        if (e.ctrlKey && e.keyCode == 13) {
            submit();
        }
    };
});