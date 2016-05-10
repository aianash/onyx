/*
 succ: function to call after success
 fail: function to call after failure
 */
function sendAjax(url, data, succ, fail) {
  $.ajax({
    url: url,
    type: 'GET',
    data: data,
  })
  .done(function(resp) {
    if(succ) succ();
  })
  .fail(function(resp) {
    if(fail) fail();
  });
}