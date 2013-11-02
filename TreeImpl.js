function TreeStruct() {
    function node(id) {
        this._id = id;
        this.previous = [];
        this.next = [];
    }

    this.dataTop = new node('');
    this.findNode = function (id, node) {
        node = node || this.dataTop;
        if (node._id === id) return node;
        else {
            for (var i = node.next.length; i--;) {
                var result = this.findNode(id, node.next[i]);
                if (result) return result;
            }
        }
    };
    this.setNode = function (id, children, checkCircularRecursion) {
        if (children && children.length) {
            var _self = this.findNode(id);
            if (!_self) {
                _self = new node(id);
                _self.previous.push(this.dataTop);
                this.dataTop.next.push(_self);
            }
            for (var j = children.length; j--;) {
                var child = this.findNode(children[j]);
                if (!child) {
                    child = new node(children[j]);
                }
                if (child.previous.indexOf(this.dataTop) !== -1) {
                    child.previous.pop();
                    this.dataTop.next.splice(this.dataTop.next.indexOf(child), 1);
                }
                child.previous.push(_self);
                if (checkCircularRecursion) {
                    if (!this.checkCircularRecursion(child) && _self !== child) {
                        if (_self.next.indexOf(child) === -1) _self.next.push(child);
                    } else {
                        child.previous.pop();
                        return false;
                    }
                } else {
                    _self.next.push(child);
                }
            }
        } else this.deleteNode(id);

        return true;
    };
    this.deleteNode = function (id) {
        var _self = this.findNode(id);
        var _n = _self.next,
            _p = _self.previous,
            i;
        for (i = _n.length; i--;) {
            _n[i].previous.splice(_n[i].previous.indexOf(_self), 1);
            if (_n[i].previous.length === 0) _n[i].previous.push(this.dataTop);
        }
        for (i = _p.length; i--;) {
            _p[i].next.splice(_p[i].next.indexOf(_self), 1);
        }
    };
    this.checkCircularRecursion = function (checkNode, currentNode) {
        if (checkNode === currentNode) return true;
        else {
            if (!currentNode) currentNode = checkNode;
            for (var k = currentNode.previous.length; k--;) {
                var result = this.checkCircularRecursion(checkNode, currentNode.previous[k]);
                if (result) return result;
            }
            return false;
        }
    };
}
