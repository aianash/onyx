window.onload = function() {
  addCallbacks();
}


function addCallbacks() {
  var id = $('#id').val();
  var intent = $('#intent').val();
  var succ = function() { location.reload(); }
  var fail = function() { $('#result').show(); }

  $('#yes').click(function() {
    sendAjax('/tag-intent', 'id=' + id + '&intent=' + intent + '&resp=yes', succ, fail)
  });

  $('#no').click(function() {
    sendAjax('/tag-intent', 'id=' + id + '&intent=' + intent + '&resp=no', succ, fail)
    location.reload();
  })
}