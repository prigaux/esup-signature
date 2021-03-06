/*
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {GlobalUi} from "./modules/globalUi.js";
let globalUi;
globalUi = new GlobalUi();
export default globalUi;

// if(document.URL.match("(\/user\/signrequests\/[\\s\\S]+[^?|^\/])")) {
//     console.info("show side bar");
//     globalUi.showSideBar();
//     $("#sidebarCollapse").attr("disabled", true);
//     // $("#sidebarCollapse").children().toggleClass("fa-bars fa-arrow-left");
//     // $("#sidebarCollapse").on("mousedown", function(e){
//     //     e.preventDefault();
//     //    document.location.href = "/user/signrequests";
//     // });
// }
//
// if(document.URL.match("(\/user\/datas\/[\\s\\S]+[^?|^\/])")) {
//     globalUi.hideSideBar();
//     $("#sidebarCollapse").attr("disabled", true);
// }

export let stepper;
let stepDiv = document.getElementById("stepperDefault");
if (stepDiv != null) {
    import('./modules/step.js').then((step) => {
        const Step = step.default;
        stepper = new Step(stepDiv);
    });
}

$(".select-users").each(function () {
    let selectId = $(this).attr('id');
    import('./modules/selectUser.js').then((selectUser) => {
        const SelectUser = selectUser.default;
        new SelectUser(selectId);
    });
});

$(".slim-select").each(function () {
    console.info("enable slim-select for : " + $(this).attr('id'));
    new SlimSelect({
        select: '#' + $(this).attr('id')
    })
})