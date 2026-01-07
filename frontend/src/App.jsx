import React from 'react';
import {BrowserRouter as Router, Route, Routes} from 'react-router-dom';
import HomePage from './pages/HomePage';
import CategoryPage from './pages/CategoryPage/index.jsx';
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
                    </Routes>
                </main>
                <Footer/>
                <ToasterConfig/>
            </div>
        </Router>
    );
};

export default App;