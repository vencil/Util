function Position(x, y) {
    this.X = x ? x : 0;
    this.Y = y ? y : 0;

    this.add = function (val) {
        if (val) {
            if (!isNaN(val.X)) this.X += val.X;
            if (!isNaN(val.Y)) this.Y += val.Y;
        }
        return this;
    };

    this.subtract = function (val) {
        if (val) {
            if (!isNaN(val.X)) this.X -= val.X;
            if (!isNaN(val.Y)) this.Y -= val.Y;
        }
        return this;
    };

    this.min = function (val) {
        if (!val) return this;
        if (!isNaN(val.X)) this.X = Math.min(this.X, val.X);
        if (!isNaN(val.Y)) this.Y = Math.min(this.Y, val.Y);
        return this;
    };

    this.max = function (val) {
        if (!val) return this;
        if (!isNaN(val.X)) this.X = Math.max(this.X, val.X);
        if (!isNaN(val.Y)) this.Y = Math.max(this.Y, val.Y);

        return this;
    };

    this.apply = function (element, control) {
       if (!element) return;
       if (!isNaN(this.X)) {
         if (!control || !(control.orientation != 'horizontal' || (control.upperLimit && this.X > control.upperLimit)
                          || (control.lowerLimit && this.X < control.lowerLimit)))
             element.style.left = this.X + 'px';
       }
       if (!isNaN(this.Y)) {
         if (!control || !(control.orientation != 'vertical' || (control.upperLimit && this.Y > control.upperLimit)
                          || (control.lowerLimit && this.Y < control.lowerLimit)))
             element.style.top = this.Y + 'px';
       }
    };
}

function absoluteCursorPosition(e) {
    e = e || window.event;
    if (isNaN(window.scrollX))
        return new Position(e.clientX + document.documentElement.scrollLeft + document.body.scrollLeft,
                                      e.clientY + document.documentElement.scrollTop + document.body.scrollTop);
    else
        return new Position(e.clientX + window.scrollX, e.clientY + window.scrollY);
}

function dragObject(element, startCallback, moveCallback, endCallback) {
    if (!element) return;

    var cursorStartPos, elementClientRect, elementStartPos;
    var dragging = false;
    var control;

    if (document.addEventListener) element.addEventListener("mousedown", dragStart, false);
    else element.attachEvent("onmousedown", dragStart);

    function dragStart(e) {
        e = e || window.event;
        if ((e.which && e.which != 1) || (e.button && e.button != 1)) return; //only allow mouse left key

        if (dragging) return;
        dragging = true;
        if (startCallback) control = startCallback(e, element);

        cursorStartPos = absoluteCursorPosition(e);
        elementClientRect = element.getBoundingClientRect();
        elementStartPos = new Position(elementClientRect.left - element.parentNode.scrollLeft, parseInt(element.style.top));
        if (document.addEventListener) {
            document.addEventListener("mousemove", dragGo, false);
            document.addEventListener("mouseup", dragStop, false);
        } else {
            document.attachEvent("onmousemove", dragGo);
            document.attachEvent("onmouseup", dragStop);
        }
    }

    function dragGo(e) {
        if (!dragging) return;
        absoluteCursorPosition(e).add(elementStartPos).subtract(cursorStartPos).apply(element, control);
        if (moveCallback) moveCallback(e, element);
    }

    function dragStop(e) {
        if (!dragging) return;
        dragging = false;
        cursorStartPos = null;
        elementStartPos = null;
        if (endCallback) endCallback(e, element);

        if (document.removeEventListener) {
            document.removeEventListener("mousemove", dragGo, false);
            document.removeEventListener("mouseup", dragStop, false);
        } else {
            document.detachEvent("onmousemove", dragGo);
            document.detachEvent("onmouseup", dragStop);
        }
    }

    this.dispose = function () {
        if (element.removeEventListener) element.removeEventListener("mousedown", dragStart, false);
        else element.detachEvent("onmousedown", dragStart);
    };
}

/*
// Usage

HTML -
<div style="background-color:green;position:relative;width:100px;" id="a">test</div>

JavaScript -
var dothis = new dragObject(document.getElementById("a"));

//after drag action is over, remove it
dothis.dispose();

//
*/