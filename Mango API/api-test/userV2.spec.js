/**
 * Copyright 2017 Infinite Automation Systems Inc.
 * http://infiniteautomation.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const config = require('@infinite-automation/mango-client/test/setup');
const MangoClient = require('@infinite-automation/mango-client');
const uuidV4 = require('uuid/v4');

function userV2Factory(client) {
    const MangoObject = client.MangoObject;
    
    return class UserV2 extends MangoObject {
        static get baseUrl() {
            return '/rest/v2/users';
        }

        static get idProperty() {
            return 'username';
        }
        
        static login(username, password, retries, retryDelay) {
            return client.restRequest({
                path: '/rest/v2/login',
                method: 'POST',
                data: {username, password},
                retries: retries || 0,
                retryDelay: retryDelay || 5000
            }).then(response => {
                return (new UserV2()).updateSelf(response);
            });
        }
        
        static logout() {
            return client.restRequest({
                path: '/rest/v2/logout',
                method: 'POST'
            });
        }

        static current() {
            return client.restRequest({
                path: this.baseUrl + '/current'
            }).then(response => {
                return (new UserV2()).updateSelf(response);
            });
        }
        
        patch(values) {
            return client.restRequest({
                path: this.constructor.baseUrl + '/' + encodeURIComponent(this[this.constructor.idProperty]),
                method: 'PATCH',
                data: values
            }).then(response => {
                return this.updateSelf(response);
            });
        }
        
        lockPassword(username) {
            return client.restRequest({
               path: this.constructor.baseUrl + '/' + encodeURIComponent(`${username}`) + '/lock-password',
               method: 'PUT'
            });
        }
    };
}
const UserV2 = userV2Factory(client);
describe('User V2 endpoint tests', function() {
    before('Login', config.login);
    
    before('Helper functions', function() {
        //Setup login using a given configuration
        this.login = function(localConfig, LocalUser) {
            this.timeout(localConfig.loginRetries * localConfig.loginRetryDelay + 5000);
            return LocalUser.login(localConfig.username, localConfig.password, localConfig.loginRetries, localConfig.loginRetryDelay);
        };
        //TODO Remove?
        this.UserV2 = userV2Factory(client);
        this.clients = {};
        this.configs = {};
    });
    
    beforeEach('Create test User', function() {
        const username = uuidV4();
        this.testUserPassword = uuidV4();
        this.testUser = new UserV2({
            name: 'name',
            username: username,
            password: this.testUserPassword,
            email: `${username}@example.com`,
            phone: '808-888-8888',
            disabled: false,
            locale: '',
            homeUrl: 'www.google.com',
            receiveAlarmEmails: 'IGNORE',
            receiveOwnAuditEvents: false,
            muted: false,
            permissions: ['testuser', 'test'],
            sessionExpirationOverride: true,
            sessionExpirationPeriod: {
                periods: 1,
                type: 'SECONDS'
            },
            organization: 'Infinite Automation Systems',
            organizationalRole: 'test engineer',
            data: {
                stringField: 'some random string',
                numberField: 123,
                booleanField: true
            }
                
        });
        
        //Save user and setup a session user for it
        return this.testUser.save().then(user => {
            assert.equal(user.username, username);
            this.configs.user = Object.assign({}, config, {
                    username: username,
                    password: this.testUserPassword
            });
            this.clients.user = new MangoClient(this.configs.user);
            this.clients.user.User = userV2Factory(this.clients.user);
            this.clients.user.user = new this.clients.user.User(user);
            return this.login(this.configs.user, this.clients.user.User);
        });
    });
    
    beforeEach('Create test admin User', function() {
        const username = uuidV4();
        this.testAdminUserPassword = uuidV4();
        this.testAdminUser = new UserV2({
            name: 'admin name',
            username: username,
            password: this.testAdminUserPassword,
            email: `${username}@example.com`,
            phone: '808-888-8888',
            disabled: false,
            locale: '',
            homeUrl: 'www.google.com',
            receiveAlarmEmails: 'IGNORE',
            receiveOwnAuditEvents: false,
            muted: false,
            permissions: ['superadmin'],
            sessionExpirationOverride: true,
            sessionExpirationPeriod: {
                periods: 1,
                type: 'HOURS'
            },
            organization: 'Infinite Automation Systems'
        });
        
        //Save user and setup a session user for it
        return this.testAdminUser.save().then(user => {
            assert.equal(user.username, username);
            this.configs.admin = Object.assign({}, config, {
                    username: username,
                    password: this.testAdminUserPassword
            });
            this.clients.admin = new MangoClient(this.configs.admin);
            this.clients.admin.User = userV2Factory(this.clients.admin);
            this.clients.admin.user = new this.clients.admin.User(user);
            return this.login(this.configs.admin, this.clients.admin.User);
        });
    });
    
    afterEach('Deletes the test user', function() {
        return this.testUser.delete().then(user => {
            assert.equal(user.username, this.testUser.username);
        });
    });
    
    afterEach('Deletes the test admin user', function() {
        return this.testAdminUser.delete().then(user => {
            assert.equal(user.username, this.testAdminUser.username);
        });
    });
    
    before('Create a session reference that uses session authentication', function() {
        this.sessionTimeoutRef = new MangoClient(config);
    });
    
    it('Gets self as non admin', function() {
        return this.clients.user.user.get().then(user => {
            assert.equal(user.username, this.testUser.username);
            assert.strictEqual(user.organization, this.testUser.organization);
            assert.strictEqual(user.organizationalRole, this.testUser.organizationalRole);
            assert.strictEqual(user.data.stringField, this.testUser.data.stringField);
            assert.strictEqual(user.data.numberField, this.testUser.data.numberField);
            assert.strictEqual(user.data.booleanField, this.testUser.data.booleanField);
            assert.isNull(user.emailVerified);
            assert.isString(user.created);
            assert.isAbove(new Date(user.created).valueOf(), 0);
            assert.include(user.permissions, 'user');
        });
    });
    
    it('Fails to create user without password', function() {
        const username = uuidV4();
        const invalidUser = new UserV2({
                name: 'name',
                username: username,
                email: `${username}@example.com`,
                phone: '808-888-8888',
                disabled: false,
                homeUrl: 'www.google.com',
                receiveAlarmEmails: 'NONE',
                receiveOwnAuditEvents: false,
                muted: false,
                locale: ''
            });
        return invalidUser.save().then(response => {
            throw new Error('Should not have created user');
        }, error => {
            assert.strictEqual(error.status, 422);
            assert.strictEqual(error.data.result.messages.length, 1);
            assert.strictEqual(error.data.result.messages[0].property, 'password');
        });
    });
    
    it('Can lock other users password as admin ', function() {
        return this.clients.admin.user.lockPassword(this.clients.user.user.username).then(response => {
            assert.strictEqual(response.status, 200);
            return this.login(this.configs.user, this.clients.user.User).then(response => {
                throw new Error('Should not have logged in');
            }, error => {
                assert.strictEqual(error.status, 401); 
            });
        });
    });
    
    it('Can\'t lock other users password as user ', function() {
        return this.clients.user.user.lockPassword(this.clients.admin.user.username).then(response => {
            throw new Error('Should not have locked password');
        }, error => {
            assert.strictEqual(error.status, 403); 
        });
    });
    
    it('Can\'t lock own password', function() {
        return this.clients.admin.user.lockPassword(this.clients.admin.user.username).then(response => {
            throw new Error('Should not have locked password');
        }, error => {
            assert.strictEqual(error.status, 403); 
        });
    });
    
    it('Can\'t make self non admin', function() {
        return this.clients.admin.user.patch({
            permissions: ['test']
        }).then(response => {
            throw new Error('Should not have updated user');
        }, error => {
           assert.strictEqual(error.status, 422);
           assert.strictEqual(error.data.result.messages.length, 1);
           assert.strictEqual(error.data.result.messages[0].property, 'permissions');
        });  
    });
    
    it('Can\'t disable self as admin', function() {
        return this.clients.admin.user.patch({
            disabled: true
        }).then(response => {
            throw new Error('Should not have updated user');
        }, error => {
           assert.strictEqual(error.status, 422); 
           assert.strictEqual(error.data.result.messages.length, 1);
           assert.strictEqual(error.data.result.messages[0].property, 'disabled');
        }); 
    });
    
    it('Can\'t disable self as user', function() {
        return this.clients.user.user.patch({
            disabled: true
        }).then(response => {
            throw new Error('Should not have updated user');
        }, error => {
           assert.strictEqual(error.status, 422); 
           assert.strictEqual(error.data.result.messages.length, 1);
           assert.strictEqual(error.data.result.messages[0].property, 'disabled');
        }); 
    });
    
    it('Can\'t update permissions as user', function() {
        return this.clients.user.user.patch({
            permissions: ['new', 'permissions']
        }).then(response => {
            throw new Error('Should not have updated user');
        }, error => {
           assert.strictEqual(error.status, 422);
           assert.strictEqual(error.status, 422); 
           assert.strictEqual(error.data.result.messages.length, 1);
           assert.strictEqual(error.data.result.messages[0].property, 'permissions');
        }); 
    });
    
    it('Can\'t rename self to existing user as user', function() {
        return this.clients.user.user.patch({
            username: this.clients.admin.user.username,
        }).then(response => {
            throw new Error('Should not have updated user');
        }, error => {
           assert.strictEqual(error.status, 422); 
           assert.strictEqual(error.data.result.messages.length, 1);
           assert.strictEqual(error.data.result.messages[0].property, 'username');
        }); 
    });

    it('Can rename self as user', function() {
        return this.clients.user.user.patch({
            username: 'Iamnew'
        }).then(user => {
            assert.strictEqual(user.username, 'Iamnew');
            this.testUser.username = 'Iamnew';
        });
    });
    
    it('Queries to match all user permissions as admin', function() {
        return this.clients.admin.User.query('permissionsContainsAll(permissions,testuser,test)').then(result => {
            assert.equal(result.total, 1);
            assert.equal(result[0].username, this.testUser.username);
        });
    });
    
    it('Queries to match one user permission as admin', function() {
        return this.clients.admin.User.query('permissionsContainsAny(permissions,testuser)').then(result => {
            assert.equal(result.total, 1);
            assert.equal(result[0].username, this.testUser.username);
        });
    });
    
    it('Queries for disabled users as admin', function() {
        //First disable the test user
        return this.testUser.patch({
            disabled: true
        }).then(user => {
            assert.equal(user.username, this.testUser.username);
            assert.equal(user.disabled, true);
            return this.clients.admin.User.query('disabled=true').then(result => {
                assert.equal(result.total, 1);
                assert.equal(result[0].username, this.testUser.username);
            });
        });
    });
    
    it('Patch test user as user', function() {
        return this.clients.user.user.patch({
            name: 'test user'
        }).then(user => {
            assert.equal(user.username, this.testUser.username);
            assert.equal(user.name, 'test user');
        });
    });
    
    it('User session timeout override expires session', function() {
        this.timeout(5000);
        const loginClient = new MangoClient(config);
        return loginClient.User.login(this.testUser.username, this.testUserPassword).then(() => {
            return config.delay(2000);
        }).then(() => {
            return loginClient.User.current().then(response => {
                throw new Error('Session should be expired');
            }, error => {
                assert.strictEqual(error.status, 401);
            });
        });
    });

    it('Non admin can update self', function() {
        this.clients.user.user.name = 'test user'; 
        return this.clients.user.user.save().then(user => {
            assert.equal(user.username, this.clients.user.user.username);
            return this.clients.user.user.get().then(user => {
                assert.strictEqual(user.name, 'test user');
            });
        });
    });
    it('Admin can update self', function() {
        this.clients.admin.user.name = 'test user'; 
        return this.clients.admin.user.save().then(user => {
            assert.equal(user.username, this.clients.admin.user.username);
            return this.clients.admin.user.get().then(user => {
                assert.strictEqual(user.name, 'test user');
            });
        });
    });
    it.skip('Can\'t manually set the emailVerified or created property when creating user');
    it.skip('Can\'t manually update the emailVerified or created property');
    it.skip('Can\'t set email to a used email address');

    it.skip('Can update own home url as non admin');
    it.skip('Can toggle audio mute setting as non admin', function() {
        return client.restRequest({
            path: `/rest/v2/users/${this.testUser.username}/mute`,
            method: 'PUT'
        }).then(response => {
            assert.equal(response.data.mute, !this.testUser.mute);
        });
    });

});
