window.onload = function() {
  addCallbacks();
}


function addCallbacks() {
  $('#yes').click(function() {
    var id = $('#id').val();
    var intent = $('#intent').val();
    var succ = function() { location.reload(); }
    var fail = function() { $('#result').show(); }
    sendAjax('/tag-intent', 'id=' + id + '&intent=' + intent, succ, fail)
  });

  $('#no').click(function() {
    location.reload();
  })
}