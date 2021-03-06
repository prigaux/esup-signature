import {PdfViewer} from "./pdfViewer.js";

export class CreateDataUi {

    constructor(documentId, fields) {
        this.pdfViewer = new PdfViewer('/user/documents/getfile/' + documentId, true, 0);
        this.pdfViewer.setDataFields(fields);
        this.pdfViewer.scale = 0.70;
        this.initListeners();
    }


    initListeners() {
        this.pdfViewer.addEventListener('ready', e => this.startRender());
        this.pdfViewer.addEventListener('render', e => this.initChangeControl()),
        document.getElementById('saveButton').addEventListener('click', e => this.saveData(e));
        document.getElementById('saveForm').addEventListener('submit', e => this.saveData(e));
    }

    initChangeControl() {
        console.info("init change control")
        let inputs = $("#newData :input");
        $.each(inputs, function() {
            console.log($(this));
            $(this).change(function () {
                let sendModalButton = $('#sendModalButton');
                sendModalButton.addClass('disabled');
                sendModalButton.removeAttr('data-target');
            });
        });
    }

    saveData(e) {
        e.preventDefault();
        console.info("check data name");
        let tempName = document.getElementById('tempName');
        if (tempName.checkValidity()) {
            console.info("submit form");
            document.getElementById('name').value = tempName.value;
            document.getElementById('newDataSubmit').click();
        } else {
            tempName.focus();
            document.getElementById('tempName');
        }
    }

    startRender() {
        this.pdfViewer.renderPage(1);
        this.pdfViewer.adjustZoom();
    }

}