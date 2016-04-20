window.onload = function() {
  var json = decodeURIComponent(document.getElementById('glimpse-conf').value);
  var canvas = init();
  drawGlimpseBoxes(canvas, json);
  addCallbacks();
}

glimpses = [];

function init() {
  var canvas = new fabric.Canvas('canvas');
  var imgElement = document.getElementById('item');
  var imgInstance = new fabric.Image(imgElement, {
    lockMovementX : true,
    lockMovementY : true,
    lockScalingX  : true,
    lockScalingY  : true,
    lockRotation  : true
  });
  canvas.setHeight(imgElement.height);
  canvas.setWidth(imgElement.width);
  canvas.add(imgInstance);
  return canvas;
}

function addCallbacks() {
  $('button').on('click', function() {
    var id = jQuery('#id').val();
    var intents = jQuery('#intents').val();
    sendAjax('/tag', 'id=' + id + '&tags=' + JSON.stringify(glimpses) + '&intents=' + JSON.stringify(intents));
  })
}

function drawGlimpseBoxes(canvas, json) {
  var obj = JSON.parse(json);
  glimpses = new Array(obj.length).fill(0);
  for(var index in obj) {
    var prop = obj[index]
    var rect = new fabric.Rect({
      top           : prop.top,
      left          : prop.left,
      width         : prop.width,
      height        : prop.height,
      lockMovementX : true,
      lockMovementY : true,
      lockScalingX  : true,
      lockScalingY  : true,
      lockRotation  : true,
      opacity       : 0.3
    });

    rect.on('selected', function(i) {
      return function() {
        this.opacity = 0.5;
        glimpses[i] = 1;
      }
    }(index));

    canvas.add(rect);
  }
}