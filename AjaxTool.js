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
