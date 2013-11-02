function getHTTPObject() {
    var xmlhttp = false;
    if (window.XMLHttpRequest) {
        xmlhttp = new XMLHttpRequest();
        // for old IE version
    } else if (window.ActiveXObject) {
        try {
            xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        }
    }
    return xmlhttp;
}

function ajaxGet(callback, url, sync) {
    var _reader = getHTTPObject();

    _reader.open('GET', url, !sync);
    if (!sync) {
        _reader.onreadystatechange = function () {
            if (_reader.readyState == 4) {
                if (_reader.status != 200) {
                    return false;
                }
                if (callback) callback(_reader);
            }
        };
    } else {
        if (_reader.status != 200) {
            return false;
        }
        if (callback) callback(_reader);
    }
    _reader.send(null);

    return true;
}

function ajaxPost(callback, url, sync, postQueryString) {
    var _reader = getHTTPObject();

    _reader.open('POST', url, !sync);
    _reader.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    if (!sync) {
        _reader.onreadystatechange = function () {
            if (_reader.readyState == 4) {
                if (_reader.status != 200) {
                    return false;
                }
                if (callback) callback(_reader);
            }
        };
    } else {
        if (_reader.status != 200) {
            return false;
        }
        if (callback) callback(_reader);
    }
    _reader.send(postQueryString);

    return true;
}


/*
**Cross Domain Use
*/

//for GET request
function crossDomainGet(url, postata, callback) {
    var head = document.getElementsByTagName('head')[0];
    var js = document.createElement('script');

    if (url.indexOf('?') === -1) url += '?' + postata;
    else url += '&' + postata;
    if (callback) url += '&callback=' + callback;
    js.setAttribute('src', url);
    if (head) {
        head.appendChild(js);
    } else {
        document.body.appendChild(js);
    }
}

//for POST request
function crossDomainPost(url, postData) {
    // Add the iframe with a unique name
    var iframe = document.createElement("iframe");
    document.body.appendChild(iframe);
    iframe.style.display = "none";
    iframe.contentWindow.name = "postIframe";

    // construct a form with hidden inputs, targeting the iframe
    var form = document.createElement("form");
    form.target = "postIframe";
    form.action = url;
    form.method = "POST";

    // repeat for each parameter
    var data = postData.split('&');
    for (var i = 0; i < data.length; i++) {
        var input = document.createElement("input");
        var _d = data[i];
        input.type = "hidden";
        input.name = _d.slice(0, _d.indexOf('='));
        input.value = _d.slice(_d.indexOf('=') + 1);
        form.appendChild(input);
    }

    document.body.appendChild(form);
    form.submit();

    //remove after post
    document.body.removeChild(form);
    iframe.onload = function () {
        document.body.removeChild(iframe);
    };
}
