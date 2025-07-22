import axios from 'axios';

export default class ApiService {

    static BASE_URL = 'http://localhost:8090/api';

    static saveToken(token) {
        localStorage.setItem('token', token);
    }

    static getToken() {
        return localStorage.getItem('token');
    }

    // save role
    static saveRole(roles) {
        localStorage.setItem('roles', JSON.stringify(roles)); //  Converts the roles object/array to a JSON string so it can be stored in localStorage.
    }

    // get roles from localStorage
    static getRoles() {
        const roles = localStorage.getItem('roles');
        return roles ? JSON.parse(roles) : null;  //  Converts the JSON string back to an object/array.
    }


    // check if the user has a specific role
    static hasRole(role) {
        const roles = this.getRoles();
        return roles ? roles.includes(role) : false;
    }

    static isAdmin() {
        return this.hasRole('ADMIN');
    }

    static isCustomer() {
        return this.hasRole('CUSTOMER');
    }

    static isDeliveryPerson() {
        return this.hasRole('DELIVERY');
    }

    static logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('roles');
    }

    static isAuthenticated() {
        const token = this.getToken();
        return !!token;  // Convert token to boolean (true if token exists, false if null/undefined/empty)
    }

    static getHeader() {
        const token = this.getToken();
        return {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    }


    // Register User
    static async registerUser(registerationData) {
        const resp = await axios.post(`${this.BASE_URL}/auth/register`, registerationData);
        return resp.data;
    }

    // Login User
    static async loginUser(loginData) {
        const resp = await axios.post(`${this.BASE_URL}/auth/login`, loginData);
        return resp.data;
    }



    /****User PROFILE MANAGEMENT SECCION**/
    static async myProfile() {
        const resp = await axios.get(`${this.BASE_URL}/users/account`, {
            headers: this.getHeader()
        })
        return resp.data;
    }

    static async updateProfile(profileData) {
        const resp = await axios.put(`${this.BASE_URL}/users/update`, profileData, {
            headers: {
                ...this.getHeader(),
                'Content-Type': 'multipart/form-data'
            }
        })
        return resp.data;
    }


    static async deactivateProfile() {
        const resp = await axios.delete(`${this.BASE_URL}/users/deactivate`, {
            headers: this.getHeader()
        });
        return resp.data;
    }


}