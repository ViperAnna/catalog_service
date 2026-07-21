import React from 'react';
import {BrowserRouter as Router, Route, Routes} from 'react-router-dom';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage/index.jsx';
import ProductsPage from './pages/ProductsPage/index.jsx';
import ProductPage from './pages/ProductPage/index.jsx';
import ProfilePage from './pages/ProfilePage/index.jsx';
import WishlistsPage from './pages/WishlistsPage/index.jsx';
import WishlistDetailPage from './pages/WishlistDetailPage/index.jsx';
import AdminUsersPage from './pages/AdminUsersPage/index.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Header from './components/layout/Header';
import Footer from './components/layout/Footer';
import ToasterConfig from './components/ui/ToasterConfig';

const App = () => {
    return (
        <Router>
            <div className="min-h-screen bg-gray-50">
                <Header/>
                <main>
                    <Routes>
                        <Route path="/" element={<HomePage/>}/>
                        <Route path="/categories/:id" element={<CategoryPage/>}/>
                        <Route path="/products" element={<ProductsPage/>}/>
                        <Route path="/products/:id" element={<ProductPage/>}/>
                        <Route
                            path="/profile"
                            element={
                                <ProtectedRoute>
                                    <ProfilePage/>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/wishlists"
                            element={
                                <ProtectedRoute>
                                    <WishlistsPage/>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/wishlists/:id"
                            element={
                                <ProtectedRoute>
                                    <WishlistDetailPage/>
                                </ProtectedRoute>
                            }
                        />
                        <Route
                            path="/admin/users"
                            element={
                                <ProtectedRoute requiredRole="ADMIN">
                                    <AdminUsersPage/>
                                </ProtectedRoute>
                            }
                        />
                    </Routes>
                </main>
                <Footer/>
                <ToasterConfig/>
            </div>
        </Router>
    );
};

export default App;
