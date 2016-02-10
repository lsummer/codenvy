/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2015] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
'use strict';

import {CodenvyNavBarCtrl} from './navbar.controller';
import {CodenvyNavBar} from './navbar.directive';
import {LogoutCtrl} from './logout/logout.controller';
import {LogoutWidget} from './logout/logout.directive';


export class CodenvyNavbarConfig {

  constructor(register) {
    register.controller('CodenvyNavBarCtrl', CodenvyNavBarCtrl);
    register.directive('codenvyNavBar', CodenvyNavBar);

    register.controller('LogoutCtrl', LogoutCtrl);
    register.directive('logoutWidget', LogoutWidget);

  }
}
