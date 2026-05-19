import React from 'react';

const EmptyImage = ({
                        size = 'large',
                        className = ''
                    }) => {
    const textSizeClasses = {
        xs: 'text-2xl',
        sm: 'text-4xl',
        medium: 'text-6xl',
        large: 'text-8xl',
        xl: 'text-9xl'
    };

    return (
        <div className="w-full h-full flex items-center justify-center bg-gray-50 rounded-lg">
            <div className={`${textSizeClasses[size]} text-blue-400 font-bold opacity-60 ${className}`}>
                ?
            </div>
        </div>
    );
};

export default EmptyImage;