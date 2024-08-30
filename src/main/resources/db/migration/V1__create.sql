CREATE DATABASE IF NOT EXISTS production;
use production;
CREATE TABLE IF NOT EXISTS user (
                                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                    name CHAR(255) NOT NULL,
                                    email CHAR(255) NOT NULL UNIQUE,
                                    username CHAR(255) NOT NULL UNIQUE,
                                    password CHAR(255) NOT NULL,
                                    active BOOLEAN NOT NULL DEFAULT FALSE,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS permission (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          name VARCHAR(255) UNIQUE NOT NULL
);


CREATE TABLE IF NOT EXISTS users_roles (
                                           user_id BIGINT,
                                           role_id BIGINT,
                                           PRIMARY KEY (user_id, role_id),
                                           FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                                           FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS roles_permissions (
                                                 role_id BIGINT,
                                                 permission_id BIGINT,
                                                 PRIMARY KEY (role_id, permission_id),
                                                 FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
                                                 FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS category (
                                        id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                        name CHAR(255) NOT NULL,
                                        description CHAR(255) NOT NULL,
                                        slug CHAR(255) NOT NULL UNIQUE,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS sub_category (
                                            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                            name CHAR(255) NOT NULL,
                                            description CHAR(255) NOT NULL,
                                            slug CHAR(255) NOT NULL UNIQUE,
                                            category_id BIGINT NOT NULL,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS product (
                                       id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                       name CHAR(255) NOT NULL,
                                       description CHAR(255) NOT NULL,
                                       slug CHAR(255) NOT NULL UNIQUE,
                                       sub_category_id BIGINT NOT NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       FOREIGN KEY (sub_category_id) REFERENCES sub_category(id)
);

CREATE TABLE IF NOT EXISTS product_image (
                                             id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                             product_id BIGINT NOT NULL,
                                             image CHAR(255) NOT NULL,
                                             type CHAR(255) NOT NULL,
                                             filename CHAR(255) NOT NULL,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                             FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS review (
                                      id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                      product_id BIGINT NOT NULL,
                                      user_id BIGINT NOT NULL,
                                      title CHAR(255) NOT NULL,
                                      description CHAR(255) NOT NULL,
                                      note INT NOT NULL,
                                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      FOREIGN KEY (product_id) REFERENCES product(id),
                                      FOREIGN KEY (user_id) REFERENCES user(id)
);


-- Inserir dados na tabela Role
INSERT INTO role (name) VALUES ('ADMIN');
INSERT INTO role (name) VALUES ('USER');
INSERT INTO role (name) VALUES ('MODERATOR');

-- Inserir dados na tabela Permission
INSERT INTO permission (name) VALUES ('READ_PRIVILEGES');
INSERT INTO permission (name) VALUES ('WRITE_PRIVILEGES');
INSERT INTO permission (name) VALUES ('DELETE_PRIVILEGES');
INSERT INTO permission (name) VALUES ('UPDATE_PRIVILEGES');

-- Associar roles com permissions
INSERT INTO roles_permissions (role_id, permission_id) VALUES
                                                           ((SELECT id FROM role WHERE name = 'ADMIN'), (SELECT id FROM permission WHERE name = 'READ_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'ADMIN'), (SELECT id FROM permission WHERE name = 'WRITE_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'ADMIN'), (SELECT id FROM permission WHERE name = 'DELETE_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'ADMIN'), (SELECT id FROM permission WHERE name = 'UPDATE_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'USER'), (SELECT id FROM permission WHERE name = 'READ_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'USER'), (SELECT id FROM permission WHERE name = 'UPDATE_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'MODERATOR'), (SELECT id FROM permission WHERE name = 'READ_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'MODERATOR'), (SELECT id FROM permission WHERE name = 'WRITE_PRIVILEGES')),
                                                           ((SELECT id FROM role WHERE name = 'MODERATOR'), (SELECT id FROM permission WHERE name = 'UPDATE_PRIVILEGES'));
