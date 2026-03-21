/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    {
      type: 'category',
      label: 'Getting Started',
      items: ['intro', 'quick-start'],
    },
    {
      type: 'category',
      label: 'Architecture',
      items: ['architecture'],
    },
    {
      type: 'category',
      label: 'Evaluation',
      items: ['metrics', 'evaluation-engine'],
    },
    {
      type: 'category',
      label: 'Integration',
      items: ['cicd-integration'],
    },
    {
      type: 'category',
      label: 'Help',
      items: ['troubleshooting'],
    },
  ],
};

module.exports = sidebars;
