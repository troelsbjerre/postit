/*jshint esversion: 6 */

function prettyPre(code) {
    return PR.prettyPrintOne('<pre class="prettyprint"><code class="language-java">'+code+'</code></pre>');
}

function prettyCode(code) {
    return PR.prettyPrintOne('<code class="language-java">'+code+'</code>');
}

let numquestions = 0;
document.addEventListener("DOMContentLoaded", () => { 
    board = document.location.href;
    board = 'postIT to ' + board.replace(/\/[^\/]*$/,"");
    document.getElementById('header').appendChild(document.createTextNode(board));
    setInterval(function () {
        console.log("Polling server.");
        let feed = document.getElementById("feed");
        fetch(document.location.href + '?f=' + numquestions)
        .then((response) => response.json())
        .then((data) => {
            console.log(data);
            data.forEach(function (text, index) {
                if (!/\S/.test(text)) {
                    numquestions++;
                    return;
                }
                let parts = decodeURIComponent(text.replace(/\+/g, ' ')).split(/\n?```\n?/);
                let div = document.createElement("div");
                div.setAttribute("class", "margin");
                for (let i = 0 ; i < parts.length ; i += 2) {
                    let subparts = parts[i].split("`");
                    for (let j = 1 ; j < subparts.length ; j += 2) {
                        subparts[j] = prettyCode(subparts[j]);
                    }
                    parts[i] = subparts.join('');
                }
                for (let i = 1 ; i < parts.length ; i += 2) {
                    parts[i] = prettyPre(parts[i]);
                }
                div.innerHTML = parts.join('');
                let li = document.createElement("li");
                li.appendChild(div);
                feed.appendChild(li);
                li.onclick = () => feed.removeChild(li);
                numquestions++;
            });
        });
    }, 1000);
});