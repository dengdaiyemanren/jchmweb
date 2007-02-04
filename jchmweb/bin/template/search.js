function findIt() { 
    var q =  document.getElementById("searchdata").value;
    if (q == "")
        return false;

//     var l = top.basefrm.document.getElementsByTagName("body"); 
//     words = unescape(q.replace(/\+/g,' ')).split(/\s+/);
//     for (w=0;w<words.length;w++) {
//         for(i=0;i<l.length;i++)
//         {
//             var pa = new RegExp("("+words[w]+")","ig");
//             l[i].innerHTML = l[i].innerHTML.replace(pa,
//                     "<span style=\"background-color:yellow;\">$1</span>");
//         }
//     }
    localSearchHighlight(top.basefrm.document, q);
}

function highlightWord(doc, node,word) {
	// Iterate into this nodes childNodes
	if (node.hasChildNodes) {
		var hi_cn;
		for (hi_cn=0;hi_cn<node.childNodes.length;hi_cn++) {
			highlightWord(doc, node.childNodes[hi_cn],word);
		}
	}

	// And do this node itself
	if (node.nodeType == 3) { // text node
		tempNodeVal = node.nodeValue.toLowerCase();
		tempWordVal = word.toLowerCase();
		if (tempNodeVal.indexOf(tempWordVal) != -1) {
			pn = node.parentNode;
			if (pn.className != "searchword") {
				// word has not already been highlighted!
				nv = node.nodeValue;
				ni = tempNodeVal.indexOf(tempWordVal);
				// Create a load of replacement nodes
				before = doc.createTextNode(nv.substr(0,ni));
				docWordVal = nv.substr(ni,word.length);
				after = doc.createTextNode(nv.substr(ni+word.length));
				hiwordtext = doc.createTextNode(docWordVal);
				hiword = doc.createElement("span");
				hiword.className = "searchword";
                hiword.style.background = "yellow";
				hiword.appendChild(hiwordtext);
				pn.insertBefore(before,node);
				pn.insertBefore(hiword,node);
				pn.insertBefore(after,node);
				pn.removeChild(node);
			}
		}
	}
}

function unhighlight() {
    unhighlightNode(top.basefrm.document.getElementsByTagName('body')[0]);
}

function unhighlightNode(node) {
	// Iterate into this nodes childNodes
	if (node.hasChildNodes) {
		var hi_cn;
		for (hi_cn=0;hi_cn<node.childNodes.length;hi_cn++) {
			unhighlightNode(node.childNodes[hi_cn]);
		}
	}

	// And do this node itself
	if (node.nodeType == 3) { // text node
		pn = node.parentNode;
		if( pn.className == "searchword" ) {
			prevSib = pn.previousSibling;
			nextSib = pn.nextSibling;
			nextSib.nodeValue = prevSib.nodeValue + node.nodeValue + nextSib.nodeValue;
			prevSib.nodeValue = '';
			node.nodeValue = '';
		}
	}
}

function localSearchHighlight(doc, searchStr) {
	if (!doc.createElement) return;
        if (searchStr == '') return;
	// Trim leading and trailing spaces after unescaping
	searchstr = unescape(searchStr).replace(/^\s+|\s+$/g, "");
	if( searchStr == '' ) return;
	phrases = searchStr.replace(/\+/g,' ').split(/\"/);
	// Use this next line if you would like to force the script to always
	// search for phrases. See below as well!!!
	//phrases = new Array(); phrases[0] = ''; phrases[1] = searchStr.replace(/\+/g,' ');
	for(p=0;p<phrases.length;p++) {
	        phrases[p] = unescape(phrases[p]).replace(/^\s+|\s+$/g, "");
		if( phrases[p] == '' ) continue;
		if( p % 2 == 0 ) words = phrases[p].replace(/([+,()]|%(29|28)|\W+(AND|OR)\W+)/g,' ').split(/\s+/);
		else { words=Array(1); words[0] = phrases[p]; }
               	for (w=0;w<words.length;w++) {
			if( words[w] == '' ) continue;
			highlightWord(doc, doc.getElementsByTagName("body")[0],words[w]);
        	}
	}
}
