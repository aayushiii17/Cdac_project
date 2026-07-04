# 🚀 CodeCollab – Real-Time Collaborative Coding Platform

## Project Overview

**CodeCollab** is a full-stack, real-time collaborative coding platform designed to enable multiple users to write, edit, execute, and discuss code simultaneously from anywhere. The platform eliminates the need for screen sharing or manual file exchange by providing a shared coding workspace with instant synchronization, live communication, and secure collaboration.

The application supports collaborative workspaces where users can invite team members, edit code together in real time, monitor participant activity through presence indicators and cursor synchronization, communicate using integrated project chat, and execute code in multiple programming languages through an online execution engine.

The system follows a modular backend architecture where each component is responsible for a specific business capability. The authentication service, developed using **Spring Boot** and **Spring Security**, provides secure user registration, login, JWT-based authentication, and role-based authorization. Real-time collaboration services leverage **WebSockets** and **Socket.IO** to synchronize code changes, user presence, and messaging with minimal latency. Code execution is integrated through the **Judge0 API**, enabling users to compile and run programs in multiple languages without requiring local installations.

The project is designed using modern software engineering principles, including layered architecture, RESTful API design, stateless authentication, secure password hashing with BCrypt, DTO-based communication, repository pattern, and scalable backend development practices. The architecture promotes modularity, maintainability, and future extensibility, making it suitable for collaborative educational environments, coding interviews, competitive programming sessions, and team-based software development.

## Key Features

* Secure user registration and login using JWT Authentication
* Role-based authorization for collaborative workspaces
* Real-time collaborative code editing
* Live cursor synchronization and user presence tracking
* Integrated project chat using WebSockets
* Multi-language code execution via Judge0 API
* Workspace and file management
* File version history and collaborative project organization
* Responsive web interface built with React
* Modular and scalable backend architecture

## Target Users

* Students
* Software Developers
* Competitive Programmers
* Technical Interviewers
* Educators and Trainers

## Technology Stack

**Frontend**

* React.js
* Monaco Editor
* Socket.IO Client
* HTML5, CSS3, JavaScript

**Backend**

* Spring Boot (Authentication Service)
* Node.js / Express.js (Collaboration Services)
* WebSockets & Socket.IO
* REST APIs

**Database**

* PostgreSQL

**Security**

* Spring Security
* JWT Authentication
* BCrypt Password Hashing

**Code Execution**

* Judge0 API

## Project Objectives

* Enable seamless real-time collaborative programming.
* Provide secure authentication and authorization for users and workspaces.
* Synchronize code changes instantly across connected users.
* Facilitate communication through integrated project chat.
* Support execution of code in multiple programming languages.
* Maintain clean, scalable, and production-oriented backend architecture suitable for future enhancements and microservice integration.
