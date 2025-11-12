import React from 'react';

const ArchitectureLayout = ({ children }: { children: React.ReactNode }) => {
  return (
    <div style={{ height: '100vh', width: '100vw' }}>
      {children}
    </div>
  );
};

export default ArchitectureLayout;