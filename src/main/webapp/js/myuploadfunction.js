function getFiles() {
    $.getJSON("/api/files").done(function (data) {
        $("tr:has(td)").remove();

        $.each(data, function (key, value) {
            $("#uploaded-files").append(
                $('<tr/>')
                    .append($('<td/>').text(value.userId))
                    .append($('<td/>').text(value.filename))
                    .append($('<td/>').text(value.status))
                    .append($('<td/>').text(value.uploadedTimeInMilliseconds))
                    .append($('<td/>').text(value.chunks))
                    .append($('<td/>').html(value.status === 'Completed' ? "<a href='" + value.links.href + "'>Click</a>" : ""))
            );
        });
    });
}

$(document).ready(function () {
    getFiles();
});

$(function () {
    $('#fileupload').bind('fileuploadsubmit', function (e, data) {
        data.formData = {userId: $('#userId').val()};
    });

    $('#fileupload').fileupload({
        dataType: 'json',
        maxChunkSize: 1000000, // 1MB = 1000000

        submit: function (e, data) {
            var filename = new Date().getTime() + "_" + data.originalFiles[0].name.trim();
            data.files[0].uploadName = filename;
        },

        chunkdone: function (e, data) {
            if ((data.uploadedBytes - data.maxChunkSize) <= 0) {
                getFiles();
            }
        },

        done: function (e, data) {
            getFiles();
        },

        progressall: function (e, data) {
            var progress = parseInt(data.loaded / data.total * 100, 10);
            $('#progress .bar').css(
                'width',
                progress + '%'
            );
        },

        fail: function (e, data) {
            alert(data.jqXHR.responseText);
            getFiles();
        },

        dropZone: $('#dropzone')
    });
});