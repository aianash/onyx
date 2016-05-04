function readImageCallback(canvas) {
  return function readImage() {
    if(this.files && this.files[0]) {
      var FR = new FileReader();
      FR.onload = function(e) {
        var img = new Image();
        img.src = event.target.result;
        img.onload = function() {
          var imgInstance = new fabric.Image(img, {
            lockMovementX : true,
            lockMovementY : true,
            lockScalingX  : true,
            lockScalingY  : true,
            lockRotation  : true
          });
          canvas.setHeight(img.height);
          canvas.setWidth(img.width);
          canvas.add(imgInstance);
        };
      };
      FR.readAsDataURL(this.files[0]);
    }
  }
}


window.onload = function() {
  var canvas = new fabric.Canvas('canvas');
  $('#fileUpload').on('change', readImageCallback(canvas));
  $('#add-box').on('click', addBoxCallback(canvas));
  $('#generate-config').on('click', collectDataCallback(canvas));
  $('#load-config').on('click', loadConfigCallback(canvas));
  $('#remove-box').on('click', removeBoxCallback(canvas));
  $('#clear-all').on('click', clearBoxesCallback(canvas));
  $('#upload-config').on('change', uploadConfigCallback(canvas));
}


function clearBoxesCallback(canvas) {
  return function clearBoxes() {
    var objects = canvas.getObjects().filter(function(o) {
      return !o.isType('image');
    });
    objects.forEach(function(o) {
      o.remove();
    });
  }
}


function uploadConfigCallback(canvas) {
  return function uploadConfig(e) {
    clearBoxesCallback(canvas)();
    var file = e.target.files[0];
    var reader = new FileReader();
    reader.onload = function(e) {
      var config = JSON.parse(reader.result);
      displayBoxes(canvas, config)
    }
    reader.readAsText(file);
  }
}

function displayBoxes(canvas, config) {
  for(var index in config) {
    var prop = config[index]
    var rect = new fabric.Rect({
      top           : prop.top,
      left          : prop.left,
      width         : prop.width,
      height        : prop.height,
      opacity       : 0.3
    });
    canvas.add(rect);
  }
}


function loadConfigCallback(canvas) {
  return function() {
    clearBoxesCallback(canvas)();
    var config = JSON.parse($('#config').val());
    displayBoxes(canvas, config);
  }
}


function collectDataCallback(canvas) {
  return function saveConfig() {
    var rects = getRects(canvas);
    window.location = '/save-config?config=' + rects;
  }
}


function getRects(canvas) {
  var objects = canvas.getObjects().filter(function(o) {
    return !o.isType('image');
  });

  var rects = objects.map(function(o) {
    return {
      'left'   : o.getLeft(),
      'top'    : o.getTop(),
      'height' : o.getHeight(),
      'width'  : o.getWidth()
    }
  });

  return JSON.stringify(rects);
}


function addBoxCallback(canvas){
  return function addBox() {
    var rect = new fabric.Rect({
      width   : 100,
      height  : 100,
      opacity : 0.2
    });
    canvas.add(rect);
  }
}

function removeBoxCallback(canvas) {
  return function() {
    console.log(canvas.getActiveObject());
    canvas.getActiveObject().remove();
  }
}