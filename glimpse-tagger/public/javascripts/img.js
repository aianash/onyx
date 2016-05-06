window.onload = function() {
  var json = decodeURIComponent(document.getElementById('glimpse-conf').value);
  var canvas = init();
  var selectedboxes = $('#selected-glimpse').val().split(',');
  drawGlimpseBoxes(canvas, json, selectedboxes);
  addCallbacks(canvas);
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
  bindKeyEvents();
  return canvas;
}

function addCallbacks(canvas) {
  $('button').on('click', function() {
    var id = jQuery('#id').val();
    var intents = [];
    $('.intents').each(function() {
      intents = intents.concat($(this).val() || [])
    });
    sendAjax('/tag', 'id=' + id + '&tags=' + JSON.stringify(glimpses) + '&intents=' + JSON.stringify(intents));
  });
}

function drawGlimpseBoxes(canvas, json, selectedboxes) {
  var obj = JSON.parse(json);
  glimpses = new Array(obj.length).fill(0);
  for(var index in obj) {
    var prop = obj[index]
    var rect = new fabric.Rect({
      top           : prop.top,
      left          : prop.left,
      width         : prop.width,
      height        : prop.height,
      hasControls   : false,
      lockMovementX : true,
      lockMovementY : true,
      lockScalingX  : true,
      lockScalingY  : true,
      lockRotation  : true,
      opacity       : 0.2
    });

    var text = new fabric.Text(index, {
      top           : prop.top,
      left          : prop.left,
      hasControls   : false,
      lockMovementX : true,
      lockMovementY : true,
      lockScalingX  : true,
      lockScalingY  : true,
      lockRotation  : true,
      fontSize      : 20
    });

    if(selectedboxes[index] == 1) {
      rect.isselected = true;
      rect.setOpacity(0.5);
      glimpses[index] = 1;
    } else {
      rect.isselected = false;
    }


    rect.on('mouseup', function(i) {
      return function() {
        if(this.isselected) {
          this.setOpacity(0.2);
          this.isselected = false;
          glimpses[i] = 0;
        } else {
          this.setOpacity(0.5);
          this.isselected = true;
          glimpses[i] = 1;
        }
        canvas.renderAll();
      }
    }(index));

    canvas.add(text);
    canvas.add(rect);
  }
}

function bindKeyEvents() {
  Mousetrap.bind('left', function() {
    $('a')[0].click();
  });

  Mousetrap.bind('right', function() {
    $('a')[1].click();
  });
}