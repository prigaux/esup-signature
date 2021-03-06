import {WorkspacePdf} from "./workspacePdf.js";

export class SignUi {

    constructor(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, isPdf, currentStepNumber, signImages) {
        console.info("Starting sign UI");
        this.signRequestId = id;
        this.percent = 0;
        this.getProgressTimer = null;
        this.wait = $('#wait');
        this.passwordError = document.getElementById("passwordError");
        this.workspace = null;
        this.signForm = document.getElementById("signForm");
        if(isPdf) {
            this.workspace = new WorkspacePdf(id, currentSignRequestParams, currentSignType, signWidth, signHeight, signable, postits, currentStepNumber, signImages);
        }
        this.xmlHttpMain = new XMLHttpRequest();
        this.signComment = $('#signComment');
        this.initListeners();
    }

    initListeners() {
        $("#launchSignButton").on('click', e => this.launchSign());
        //$("#launchAllSignButton").on('click', e => this.launchAllSign());
        $("#password").on('keyup', function (e) {
            if (e.keyCode === 13) {
                $("#launchSignButton").click();
            }
        });
    }

    launchSign() {
        $('#signModal').modal('hide');
        this.percent = 0;
        let inputs = this.signForm.getElementsByTagName("input");
        let good = true;
        for(var i = 0, len = inputs.length; i < len; i++) {
            let input = inputs[i];
            if(!input.checkValidity()) {
                good = false;
            }
        }
        if(good) {
            console.log('launch sign for : ' + this.signRequestId);
            this.wait.modal('show');
            this.wait.modal({backdrop: 'static', keyboard: false});
            this.submitSignRequest();
        } else {
            $('#signModal').on('hidden.bs.modal', function () {
                $("#checkDataSubmit").click();
            })
        }
    }

    // launchAllSign() {
    //     $('#signAllModal').modal('hide');
    //     this.wait.modal('show');
    //     this.wait.modal({backdrop: 'static', keyboard: false});
    //     let csrf = document.getElementsByName("_csrf")[0];
    //     let signRequestParams = "password=" + document.getElementById("passwordAll").value +
    //         "&" + csrf.name + "=" + csrf.value;
    //     let xmlHttp = new XMLHttpRequest();
    //     xmlHttp.open('POST', '/user/signbooks/sign/' + this.signRequestId, true);
    //     xmlHttp.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
    //     xmlHttp.send(signRequestParams);
    // }

    submitSignRequest() {
        var formData = { };
        $.each($('#signForm').serializeArray(), function() {

            formData[this.name] = this.value;
        });
        console.log(formData);
        let csrf = document.getElementsByName("_csrf")[0];
        let signRequestUrlParams;
        if(this.workspace != null) {
            signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&signImageNumber=" + this.workspace.signPosition.signImageNumber +
                "&addDate=" + this.workspace.signPosition.dateActive +
                "&visual=" + this.workspace.signPosition.visualActive +
                "&comment=" + this.signComment.val() +
                "&xPos=" + Math.round(this.workspace.signPosition.getPdfXpos()) +
                "&yPos=" + Math.round(this.workspace.signPosition.getPdfYpos()) +
                "&signWidth=" + Math.round(this.workspace.signPosition.signWidth * 0.75 / (this.workspace.signPosition.currentScale)) +
                "&signHeight=" + Math.round(this.workspace.signPosition.signHeight * 0.75 / (this.workspace.signPosition.currentScale)) +
                "&signPageNumber=" + this.workspace.pdfViewer.pageNum +
                "&formData=" + JSON.stringify(formData) +
                "&" + csrf.name + "=" + csrf.value
            ;
        } else {
            signRequestUrlParams = "password=" + document.getElementById("password").value +
                "&" + csrf.name + "=" + csrf.value;
        }
        console.info("params to send : " + signRequestUrlParams);
        this.sendData(signRequestUrlParams);
    }

    sendData(signRequestUrlParams) {
        this.reset();
        this.xmlHttpMain.open('POST', '/user/signrequests/sign/' + this.signRequestId, true);
        this.xmlHttpMain.addEventListener('readystatechange', e => this.end());
        this.xmlHttpMain.setRequestHeader('Content-Type','application/x-www-form-urlencoded');
        // this.xmlHttpMain.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
        this.xmlHttpMain.send(signRequestUrlParams);
        this.getProgressTimer = setInterval(e => this.getStep(), 500);
    }

    getStep() {
        this.percent = this.percent + 2;
        let xmlHttp = new XMLHttpRequest();
        xmlHttp.open("GET", "/user/signrequests/get-step", true);
        xmlHttp.addEventListener('readystatechange', e => this.updateWaitModal(xmlHttp));
        xmlHttp.send(null);
    }

    reset() {
        this.percent = 0;
        this.passwordError.style.display = "none";
        document.getElementById("signError").style.display = "none";
        document.getElementById("closeModal").style.display = "none";
        document.getElementById("validModal").style.display = "none";
        document.getElementById("bar").style.display = "none";
        document.getElementById("bar").classList.add("progress-bar-animated");
    }

    updateWaitModal(xmlHttp) {
        let result = xmlHttp.responseText;
        console.log("getStep : " + result);
        if (result === "security_bad_password") {
            console.error("bad password");
            this.passwordError.style.display = "block";
            clearInterval(this.getProgressTimer);
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
        } else if(result === "sign_system_error" || result === "not_authorized") {
            console.error("bad password");
            clearInterval(this.getProgressTimer);
            document.getElementById("signError").style.display = "block";
            document.getElementById("closeModal").style.display = "block";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            this.reset();
        } else if(result === "initNexu") {
            console.info("redirect to NexU sign proccess");
            clearInterval(this.getProgressTimer);
            document.location.href="/user/nexu-sign/" + this.signRequestId;
        }else if(result === "end") {
            console.info("get end");
            clearInterval(this.getProgressTimer);
            document.getElementById("bar-text").innerHTML = "";
            document.getElementById("bar").classList.remove("progress-bar-animated");
            document.getElementById("bar-text").innerHTML = "Signature terminée";
            document.getElementById("bar").style.width = 100 + "%";
        } else if(result !== ""){
            console.debug("update bar : " + result);
            document.getElementById("bar").style.display = "block";
            document.getElementById("bar").style.width = this.percent + "%";
            document.getElementById("bar-text").innerHTML = result;
        }
    }

    end() {
        if(this.xmlHttpMain.status === 200) {
            console.info("sign end");
            document.getElementById("validModal").style.display = "block";
            setTimeout(e => this.redirect(),500);
        }
    }

    redirect() {
        document.location.href="/user/signrequests/" + this.signRequestId;
    }
}