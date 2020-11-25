/**
 * Wrap setTimeout as a Promise
 * @param wait - as wait milliseconds in setTimeout
 * @param [value]
 * @returns {Promise<any>}
 */
function delay(wait, value) {
    return new Promise(function (resolve) {
        setTimeout(resolve, wait, value);
    });
}

/**
 * Wrap native prompt as a promise.
 * Note: native prompt may get banned by browser security setting or web-site setting like iframe policy,
 * We should use custom modal dialog as possible as we can.
 * @param message
 * @param defaultValue
 * @returns {Promise}
 */
function nativePromptPromise(message, defaultValue) {
    return new Promise(function (resolve, reject) {
        const value = prompt(message, defaultValue);
        if (value.length) {
            return resolve(value);
        } else {
            return reject(false);
        }
    });
}

/**
 * Wrap native confirm as a promise.
 * Note: native confirm may get banned by browser security setting or web-site setting like iframe policy,
 * We should use custom modal dialog as possible as we can.
 * @param message
 * @returns {Promise}
 */
function nativeConfirmPromise(message) {
    return new Promise(function (resolve, reject) {
        const confirmed = confirm(message);
        return confirmed ? resolve(true) : reject(false);
    });
}

/**
 * create a basic asynchronous request
 * @param options - {method, url, [headers], [responseType], [data]}
 * @returns {Promise<XMLHttpRequest.response>}
 */
function createPromiseRequest(options) {
    return new Promise(function (resolve, reject) {
        let xhr = new XMLHttpRequest();
        xhr.open(options.method, options.url);
        xhr.onload = function () {
            if (this.status >= 200 && this.status < 300) {
                resolve(xhr.response);
            } else {
                reject({
                    status: this.status,
                    statusText: xhr.statusText,
                    response: xhr.response
                });
            }
        };
        xhr.onerror = function () {
            reject({
                status: this.status,
                statusText: xhr.statusText
            });
        };

        if (options.hasOwnProperty("headers")) {
            Object.keys(options.headers).forEach(function (key) {
                xhr.setRequestHeader(key, options.headers[key]);
            });
        }

        if (options.hasOwnProperty("responseType")) {
            xhr.responseType = options.responseType;
        }

        let data = options.data || null;
        xhr.send(data);
    });
}

/**
 * Send a get method request and get a response promise
 * @param url
 * @param data
 * @returns {Promise<XMLHttpRequest.response>}
 */
function getPromise(url, data) {
    // Turn the data object into an array of URL-encoded key/value pairs.
    data = data || {};
    let urlEncodedDataPairs = Object.entries(data).reduce(function (accumulator, pair) {
        const key = pair[0], value = pair[1];
        if (Array.isArray(value)) {
            return accumulator.concat(value.map(function (subValue) {
                return encodeURIComponent(key) + '=' + encodeURIComponent(subValue);
            }));
        } else {
            accumulator.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
            return accumulator;
        }
    }, []);

    if (urlEncodedDataPairs.length) {
        url += '?' + urlEncodedDataPairs.join('&');
    }

    return createPromiseRequest({
        "method": "GET",
        "url": url
    });
}

/**
 * Send a post method request and get a response promise
 * @param url
 * @param data
 * @returns {Promise<XMLHttpRequest.response>}
 */
function postPromise(url, data) {
    // Turn the data object into an array of URL-encoded key/value pairs.
    data = data || {};
    let urlEncodedDataPairs = Object.entries(data).reduce(function (accumulator, pair) {
        const key = pair[0], value = pair[1];
        if (Array.isArray(value)) {
            return accumulator.concat(value.map(function (subValue) {
                return encodeURIComponent(key) + '=' + encodeURIComponent(subValue);
            }));
        } else {
            accumulator.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
            return accumulator;
        }
    }, []);

    // x-www-form-urlencoded issue.
    // Combine the pairs into a single string and replace all %-encoded spaces to
    // the '+' character; matches the behaviour of browser form submissions.
    let urlEncodedData = urlEncodedDataPairs.join('&').replace(/%20/g, '+');

    return createPromiseRequest({
        "method": "POST",
        "url": url,
        "headers": {'Content-Type': 'application/x-www-form-urlencoded'},
        "data": urlEncodedData
    });
}

/**
 * Send a FormData object and get a response promise.
 * With the FormData object we are able to send uploaded blob.
 * @param url
 * @param formData.
 * @returns {Promise<XMLHttpRequest.response>}
 */
function postFormDataPromise(url, formData) {
    return createPromiseRequest({
        "method": "POST",
        "url": url,
        "data": formData
    });
}

/**
 * Wrap the image to dataURL process with Promise. Return a base64 encoded string promise.
 * @param blob. The Blob or File from which to read.
 * @returns {Promise<string>}
 */
function readBlobAsDataURL(blob) {
    return new Promise(function (resolve, reject) {
        const reader = new FileReader();
        reader.onloadend = function (event) {
            return resolve(event.target.result);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
    });
}