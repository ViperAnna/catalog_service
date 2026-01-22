import React from 'react';
import {Toaster} from 'react-hot-toast';

const ToasterConfig = () => {
    return (
        <Toaster
            position="top-center"
            toastOptions={{
                success: {
                    duration: 5000,
                    icon: '✅',
                    style: {
                        background: '#10b981',
                        color: 'white',
                    },
                },
                error: {
                    duration: 5000,
                    icon: '❌',
                    style: {
                        background: '#ef4444',
                        color: 'white',
                    },
                },
            }}
        />
    );
};

export default ToasterConfig;